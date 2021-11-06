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
class Condition {
    var con:String
    constructor(con:String){
        this.con = con
    }
    infix fun <K,V>and(x:KProperty1<K,V>):Condition {
        con = "$con and ${x.name}"
        return this
    }
    infix fun <K,V>or(x:KProperty1<K,V>):Condition {
        con = "$con or ${x.name}"
        return this
    }
    infix fun limit(x: Int): Condition {
        con = "$con limit $x"
        return this
    }

    /*
    运算符 =
     */
    infix fun equal(x: String): Condition {
        con = "$con = ${x}"
        return this
    }
    infix fun equal(x: Int): Condition {
        con = "$con = ${x}"
        return this
    }
    infix fun equal(x: Double): Condition {
        con = "$con = ${x}"
        return this
    }
    /*
    运算符 >
     */
    infix fun greater(x:Int ): Condition {
        con = "$con > ${x}"
        return this
    }
    infix fun greater(x:Double ): Condition {
        con = "$con > ${x}"
        return this
    }
    /*
     运算符 <
     */
    infix fun less(x: Int): Condition {
        con = "$con < ${x}"
        return this
    }
    infix fun less(x: Double): Condition {
        con = "$con < ${x}"
        return this
    }
}
/*
运算符 ==
 */
infix fun <K,V>KProperty1<K,V>.equal(x:String):Condition{
    val str = "${this.name} = '$x'"
    val con = Condition(str)
    return con
}
infix fun <K,V>KProperty1<K,V>.equal(x:Int):Condition{
    val str = "${this.name} = '$x'"
    val con = Condition(str)
    return con
}
infix fun <K,V>KProperty1<K,V>.equal(x:Double):Condition{
    val str = "${this.name} = '$x'"
    val con = Condition(str)
    return con
}

/*
运算符 >
 */
infix fun <K,V>KProperty1<K,V>.greater(x: Int): Condition {
    val str = "${this.name} > $x"
    val con = Condition(str)
    return con
}
infix fun <K,V>KProperty1<K,V>.greater(x: Double): Condition {
    val str = "${this.name} > $x"
    val con = Condition(str)
    return con
}
/*
运算符 <
 */
infix fun <K,V>KProperty1<K,V>.less(x: Int): Condition {
    val str = "${this.name} < $x"
    val con = Condition(str)
    return con
}
infix fun <K,V>KProperty1<K,V>.less(x: Double): Condition {
    val str = "${this.name} < $x"
    val con = Condition(str)
    return con
}


/*
运算符 个数
 */
fun limit(x: Int): Condition {
    val str = "limit $x"
    val con = Condition(str)
    return con

}

/*
运算符 全部
 */
fun all(): Condition {
    val str = ""
    val con = Condition(str)
    return con
}


/*
数据库查操作
返回值数组
 */
inline fun <reified T>DBServer.select(call:()->Condition) : List<T>?{

    val array = DBSQL.select(table,call().con)
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
inline fun <reified T>DBServer.selectOne(call:()->Condition) : T?{
    val array = DBSQL.select(table,call().con)
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
inline fun <reified T>DBServer.update(data:(T)->Void,call:()->Condition){

    val model = db_new<T>()
    data(model)
    if (model is JSONInterface){
        DBSQL.update(table,toJson(model as JSONInterface),call().con)
    }

}
/*
数据库删除操作
 */
inline fun DBServer.delete(call:()->Condition){
   DBSQL.delete(table,call().con)

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

