package com.example.myapplication

import android.content.Context
import android.util.Log
import kotlin.reflect.*
import kotlin.reflect.full.declaredMemberProperties

/*
接口类
 */
public class DBServer {
    val table:String
    constructor(table:String){
        this.table = table
    }
    companion object {
        /*
        设置上下文
         */
        public fun setupContext(c: Context) {
            DBSQL.setupContext(c)
        }

        /*
        注册数据库表
         */
        public fun register(obj:JSONInterface){
            obj::class.declaredMemberProperties.forEach{ prop ->
                prop.annotations.forEach{
                    if (it is DBJSON) {
                        val t = it as DBJSON
                        val table =  t.table
                        val key =  t.key
                        DBSQL.create(table)
                        DBSQL.alter(table,key)
                    }
                }
            }
        }
    }

}

/*
运算符 == 参数string
 */
infix fun String.equal(x: String): String {
    return this + " = " + "'$x'"
}
/*
运算符 == 参数double
 */
infix fun String.equal(x: Double): String {
    return this + " = " + "$x"
}
/*
运算符 an
 */
infix fun String.and(x: String): String {
    return this + " and " + x
}
/*
运算符 or
 */
infix fun String.or(x: String): String {
    return this + " or " + x
}

infix fun String.limit(x: Int): String {
    return "limit $x"
}

/*
运算符 >
 */
infix fun String.greater(x: Double): String {
    return "$this > $x"
}

/*
运算符 <
 */
infix fun String.less(x: Double): String {
    return "$this < $x"
}

/*
运算符 >
 */
infix fun String.greater(x: Int): String {
    return "$this > $x"
}

/*
运算符 <
 */
infix fun String.less(x: Int): String {
    return "$this < $x"
}
/*
运算符 个数
 */
fun limit(x: Int): String {
    return "limit $x"
}

/*
运算符 全部
 */
fun all(): String {
    return ""
}

/*
数据库查操作
返回值数组
 */
inline fun <reified T>DBServer.select(call:()->String) : List<T>?{

    val array = DBSQL.select(table,call())
    var list = mutableListOf<T>()
    if (array == null ){return null}
    if (array.count() == 0 ){return null}

    for (item in array) {
        val dic = item as Map<String,Any>
        val model = toModel<T>(dic)
        list.add(model!!)
    }
    return list
}

/*
数据库查操作
返回单个对象实例
 */
inline fun <reified T>DBServer.selectOne(call:()->String) : T?{
    val array = DBSQL.select(table,call())
    if (array == null){return null}
    if (array?.count() > 0){
        val dic = array.get(0) as Map<String,Any>
        val model = toModel<T>(dic)
        return model
    }
    return null
}

/*
数据库插入操作
 */
inline fun <reified T:JSONInterface>DBServer.insert(data: (T) -> Unit){
    val model = db_new<T>()
    data(model)
    if (model is JSONInterface){
        DBSQL.insert(table, toJson(model as JSONInterface))
    }

}
/*
数据库插入操作
参数：一个对象
 */
inline fun <reified T>DBServer.insert(data:JSONInterface){
    DBSQL.insert(table, data.toJson())
}

/*
数据库更新操作
 */
inline fun <reified T>DBServer.update(data:(T)->Void,call:()->String){

    val model = db_new<T>()
    data(model)
    if (model is JSONInterface){
        DBSQL.update(table,toJson(model as JSONInterface),call())
    }

}
/*
数据库删除操作
 */
inline fun DBServer.delete(call:()->String){
   DBSQL.delete(table,call())

}
/*
数据库提交操作  增，删，改 会延迟执行，想要立即同步数据库，调用本方法
 */
inline fun DBServer.commit(){
    DBSQL.commit()
}

/*
工具 实例化
 */
inline fun <reified T>db_new(): T{
    val clz = T::class.java
    val mCreate = clz.getDeclaredConstructor()
    return mCreate.newInstance()
}

/*
工具 json转model
 */
inline fun <reified T>toModel(json:Map<String,Any>) :T?{
    val clz = T::class.java
    val mCreate = clz.getDeclaredConstructor()
    val obj = mCreate.newInstance()
    val js = obj as? JSONInterface
    js?.toModel(json)
    return js as? T
}
/*
工具 model转json
 */
fun toJson(model:JSONInterface) :Map<String,Any>{
    return model.toJson()
}

/*
注解类
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class DBJSON(val table:String,val key:String)

/*
 kotlin反射,解析数据赋值
*/
interface JSONInterface {

    fun toModel(json:Map<String,Any>){
        this::class.declaredMemberProperties.forEach{ prop ->
            prop.annotations.forEach{
                if (it is DBJSON) {
                    val t = it as DBJSON
                    val value =  json?.get(t.key)
                    val mutProp = prop as KMutableProperty1?
                    if (value != null ) {
                        if (prop.returnType.classifier == Int::class){
                            mutProp?.setter?.call(this,"$value".toInt())
                        }else if (prop.returnType.classifier == Float::class) {
                            mutProp?.setter?.call(this, "$value".toFloat())
                        }else if (prop.returnType.classifier == Double::class) {
                            mutProp?.setter?.call(this, "$value".toDouble())
                        }else if (prop.returnType.classifier == Long::class) {
                            mutProp?.setter?.call(this, "$value".toLong())
                        }else if (prop.returnType.classifier == String::class) {
                            mutProp?.setter?.call(this, "$value")
                        }

                    }
                }

            }
        }
    }
    fun toJson() : Map<String,Any> {
        var json = mutableMapOf<String,Any>()
        this::class.declaredMemberProperties.forEach{ prop ->
            prop.annotations.forEach{
                if (it is DBJSON) {
                    val t = it as DBJSON
                    val mutProp = prop as KMutableProperty1?
                    val value = mutProp?.getter?.call(this)
                    if (value != null) {
                        json.put(t.key,value !!)
                    }
                }
            }
        }
        return json
    }
}

