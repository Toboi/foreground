package me.bgregos.foreground.task

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

data class Task(var name:String) : Serializable {
    var uuid:UUID = UUID.randomUUID()
    var dueDate:Date? = null
    var createdDate:Date = Date()
    var project:String? = null
    var tags:ArrayList<String> = ArrayList()
    var modifiedDate:Date? = null
    var priority: String? = ""
    var status: String = "pending"
    var others = mutableMapOf<String, String>() //unaccounted-for fields. (User Defined Attributes)
    //List of all possible Task Statuses at https://taskwarrior.org/docs/design/task.html#attr_status

    companion object {
        fun shouldDisplay(task:Task):Boolean{
            if (!(task.status=="completed" || task.status=="deleted" || task.status=="recurring" || task.status=="waiting"))
                return true
            val convDate = task.others.get("wait")?:""
            Log.e(this.javaClass.toString(), "jsontags: "+convDate.toString())
            if (!convDate.isNullOrEmpty()) {
                val date = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'").parse(convDate)
                if (date.after(Date()) && task.status=="waiting"){
                    task.status="pending"
                    return true
                }
            }
            return false
        }
        fun toJson(task:Task):String{
            Log.v("brighttask", "tojsoning: "+task.name)
            val out = JSONObject()
            out.putOpt("description", task.name)
            out.put("uuid", task.uuid)
            out.putOpt("project", task.project)
            out.putOpt("status", task.status)
            out.putOpt("priority", task.priority)
            if (task.dueDate!=null) {
                out.putOpt("due", SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'").format(task.dueDate))
            }
            if (task.modifiedDate!=null) {
                out.putOpt("modified", SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'").format(task.modifiedDate))
            }
            out.putOpt("created", SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'").format(task.createdDate))
            out.putOpt("tags", JSONArray(task.tags))

            for(extra in task.others){
                out.putOpt(extra.key, extra.value)
            }

            JSONObject(out.toString()) //for testing, remove


            return out.toString()
        }

        fun fromJson(json:String):Task?{
            val out = Task("")
            var obj = JSONObject()
            try {
                obj = JSONObject(json)
            } catch (ex:Exception){
                Log.e(this.javaClass.toString(), "Skipping task import: "+ex.toString())
                return null
            }
            out.name = obj.optString("description")?: ""
            out.uuid = UUID.fromString(obj.getString("uuid"))
            out.project = obj.optString("project") ?: null
            out.status = obj.optString("status") ?: "pending"
            out.priority = obj.optString("priority") ?: null
            var convDate : String? = null
            convDate = obj.optString("due")?:""
            if (!convDate.isNullOrBlank()) {
                out.dueDate = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'").parse(convDate)
            }
            convDate = obj.optString("modified")?:""
            if (!convDate.isNullOrBlank()) {
                out.modifiedDate = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'").parse(convDate)
            }
            convDate = obj.optString("created")?:""
            Log.v("sanity-check", "created date from import: "+convDate+";")
            if (!convDate.isNullOrBlank()) {
                out.createdDate = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'").parse(convDate)
            }
            val jsontags = obj.optJSONArray("tags")?: JSONArray()

            for (j in 0 until jsontags.length()) {
                out.tags.add(jsontags.getString(j))
            }
            //remove what we have specific fields for
            obj.remove("description")
            obj.remove("uuid")
            obj.remove("project")
            obj.remove("status")
            obj.remove("priority")
            obj.remove("due")
            obj.remove("modified")
            obj.remove("created")
            obj.remove("tags")
            //add all others to the others map
            for (key in obj.keys()){
                out.others[key] =  obj.getString(key)
            }
            return out
        }
    }

}