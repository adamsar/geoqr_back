package models

import com.mongodb.casbah.Imports._
import play.api.libs.json._

class Location(val id: Option[String], val code: String,
			   val location: List[Double],
			   val info: String, val timeToExpires: Int) 
	extends StoredInMongo{

  def baseMap:scala.collection.mutable.Map[String, Any] =
    scala.collection.mutable.Map(
      "code" -> code,
      "location" -> location,
      "info" -> info,
      "timeToExpires" -> timeToExpires
    )

  override def asMap:Option[scala.collection.mutable.Map[String, Any]] = id match {
    case Some(_id) => {
      val original = baseMap
      original("id") = _id
      return Some(original)
    }
    case None => Some(baseMap)
  }
}

object LocationWarehouse extends DataWarehouse[Location]{
  override def colName = "locations"
    
  //Make sure we can do geoqueries on the object
  col.ensureIndex(MongoDBObject("location" -> "2d"))
  
  def asModel(obj: DBObject):Option[Location] = {
    return Some(new Location(Some(obj.get("_id").toString),
    						 obj.get("code").asInstanceOf[String],
    						 obj.get("location").asInstanceOf[BasicDBList].toList.asInstanceOf[List[Double]],
                             obj.get("info").toString,
                             obj.get("timeToExpires").asInstanceOf[Int])
    )
  }

  def reads(json: JsValue): JsResult[Location] = JsSuccess(
    new Location(Some((json \ "id").as[String]),
    			 (json \ "code").as[String],
    			 (json \ "location").asInstanceOf[List[Double]],
                 (json \ "info").as[String],
                 (json \ "timeToExpires").as[Int])
  )

  def writes(location: Location):JsValue = {
    val _id:JsValue = location.id match {
      case Some(_id) => JsString(_id)
      case None => JsNull
    }

    return JsObject(List(
                      "id" -> _id,
                      "code" -> JsString(location.code),
                      "lat" -> JsNumber(location.location(0)),
                      "lon" -> JsNumber(location.location(1)),
                      "info" -> JsString(location.info),
                      "timeToExpires" -> JsNumber(location.timeToExpires)
                    ))
  }
}
