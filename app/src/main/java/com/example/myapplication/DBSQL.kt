package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

/*
自定义 DBSQLHelper
 */
class DBSQLHelper(var context: Context) : SQLiteOpenHelper(context,"db_sql.db",null,1) {

    override fun onCreate(db: SQLiteDatabase?) {
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    }
}
/*
数据库操作类
 */
class DBSQL{
    companion object{
        private val instance = DBSQL()
        private var edit:SharedPreferences.Editor? = null
        private var shared:SharedPreferences? = null

        /*
        设置上下文
        */
        public fun setupContext(c: Context){
            val helper = DBSQLHelper(c)
            instance.db = helper.writableDatabase
            shared = c.getSharedPreferences("default", Context.MODE_PRIVATE)
            edit = shared?.edit()

        }

        /*
        插入操作
        */
        public fun insert(tableName:String,json:Map<String,Any>){
            val sql = instance.insert(tableName,json)
            instance.addExecSQL(sql)
        }
        /*
        更新操作
        */
        public fun update(tableName:String,json:Map<String,Any>,condition:String) {
            val sql = instance.update(tableName,json,condition)
            instance.addExecSQL(sql)
        }
        /*
        删除操作
        */
        public fun delete(tableName:String,condition:String){
            val sql = instance.delete(tableName,condition)
            instance.addExecSQL(sql)
        }
        /*
        查找操作
        */
        public fun select(tableName:String,condition:String):List<Any>?{
            val sql = instance.select(tableName,condition)
            return instance.query(sql)
        }
        /*
        提交操作
        */
        public fun commit(){
            instance.commit()
        }
        /*
         创建表
        */
        public fun create(tableName:String) {
           instance.create(tableName)
        }
        /*
         增加字段
        */
        public fun alter(tableName:String,key:String) {
            instance.alter(tableName,key)


        }
        /*
         删除表
        */
        public fun drop(tableName:String) {
            instance.drop(tableName)
        }

    }
    private var db: SQLiteDatabase? = null

    /*
     执行sql添加到本地文件，延迟执行
     减少IO
    */
    private fun addExecSQL(sql:String){

        var list = shared?.getStringSet("db_sql",null)
        if (list == null){
            list = mutableSetOf<String>()
        }
        if (list.count() >= 10) {
            db?.beginTransaction();
            try {
                list.forEach { db?.execSQL(it)  }
                db?.setTransactionSuccessful();
            } finally {
                db?.endTransaction();
            }
        }
        list.add(sql)

        edit?.putStringSet("db_sql",list)
        edit?.commit()


    }
    /*
     解析条件字符串
    */
    private fun isLimit(co:String) : String{
        if (co.startsWith("limit") ){
            return co
        }
        return "where $co"
    }
    /*
     数据库延迟提交
    */
    private fun commit(){

        var list = shared?.getStringSet("db_sql",null)

        if (list == null){return}
        db?.beginTransaction();
        try {

            list.forEach {
                db?.execSQL(it)
            }
            db?.setTransactionSuccessful();
        } finally {
            db?.endTransaction();
        }
    }
    /*
     创建表
    */
    private fun create(tableName:String){

        val sql = "create table if not exists $tableName (t_id integer)"
        db?.execSQL(sql)

    }
    /*
     增加字段
    */
    private fun alter(tableName:String,key:String){
        val q_sql = "SELECT * from sqlite_master where name = '$tableName' and sql like '%$key%'"
        if (instance.queryCount(q_sql) >= 1 ){
            return
        }
        val sql = "alter table $tableName add $key text"
        db?.execSQL(sql)

    }
    /*
     删除字段
    */
    private fun drop(tableName:String){
        val sql = "drop table $tableName"
        db?.execSQL(sql)
    }
    /*
     插入sql
    */
    private fun insert(tableName:String,json:Map<String,Any>):String{

        var keys = ""
        var values = ""
        for ((k, v) in json) {
            if (keys.length == 0){
                keys = "$k"
            }else {
                keys = keys + "," + "'$k'"
            }
            if (values.length == 0){
                values = "'$v'"
            }else {
                values = values + "," + "'$v'"
            }
        }
        val sql = "insert into $tableName ($keys) values ($values)"
        return sql
    }
    /*
     更新sql
    */
    private fun update(tableName:String,json:Map<String,Any>,condition:String):String{

        var kv = ""
        for ((k,v) in json ){
            if (kv.length == 0){
                kv = "$k) = '\$v)'"
            }else {
                kv = kv + ", " + "\$k = '$v'"
            }
        }
        val con = isLimit(condition)
        val sql = "update $tableName set $kv $con"
        return sql
    }
    /*
     删除sql
    */
    private fun delete(tableName:String,condition:String):String{
        var sql = "delete from $tableName"
        if (condition.length > 0 ){
            val con = isLimit(condition)
            sql = "delete from $tableName $con"
        }
        return sql
    }
    /*
     查找sql
    */
    private fun select(tableName:String,condition:String):String{
        var sql = "select * from $tableName"
        if (condition.length > 0 ){
            val con = isLimit(condition)
            sql = "select * from $tableName $con"
        }
        return sql
    }
    /*
     查找操作
     */
    private fun query(sql:String):List<Any>?{

        if (db == null){return null}
        val arrray = mutableListOf<Any>()
        val cursor = db?.rawQuery(sql,null)
        if (cursor == null){return null}
        if (cursor.moveToFirst()){
            do {
                var dic = mutableMapOf<String,Any>()
                for (key in cursor.columnNames) {
                    val value = cursor.getString(cursor.getColumnIndex(key))
                    if (value != null){
                        dic.put(key,value)
                    }
                }
                arrray.add(dic)
            }while (cursor.moveToNext())
        }
        return  arrray
    }
    /*
     查找个数
    */
    private fun queryCount(sql:String):Int{
        var index = 0
        if (db == null){return index}
        val cursor = db?.rawQuery(sql,null)
        if (cursor == null){return index}
        if (cursor.moveToFirst()){
            do {
                index += 1
            }while (cursor.moveToNext())
        }
        return  index
    }

}
