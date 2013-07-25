package models

import com.mongodb.casbah.Imports._
import play.api.libs.json._
import java.util.Date

class CheckIn(val locationId: String,
			  val createdOn: Date,
			  val expiresOn: Date,
			  val validated: Boolean) extends StoredInMongo{
  override def asMap:Option[scala.collection.mutable.Map[String, Any]] = 
    Some(scala.collection.mutable.Map(
        "location" -> locationId.toString,
        "createdOn" -> AccountWarehouse.isoFormat.format(createdOn),
        "expiresOn" -> AccountWarehouse.isoFormat.format(expiresOn),
        "validated" -> validated
        )
    )
}

/**
 * A basic account in the system. Interesting to note
 * here that each account holds a list of checkins the user has done.
 */
class Account(val id: Option[String], 
			  val email: String, 
			  val password:String,
			  var checkIns: List[CheckIn]) extends StoredInMongo{
  
  def baseMap:scala.collection.mutable.Map[String, Any] = scala.collection.mutable.Map(
      "email" -> email,
      "password" -> password,
      "checkIns" -> checkIns.map(_.asMap) 
      )
      
  override def asMap:Option[scala.collection.mutable.Map[String, Any]] = id match {
    case Some(_id) => {
      val fullMap = baseMap
      fullMap("_id") = _id
      return Some(fullMap)
    }
    case _ => Some(baseMap)
  }
   
}

object AccountWarehouse extends DataWarehouse[Account]{
  override def colName = "accounts"
    
  def asModel(obj:DBObject):Option[Account] = {
    if(obj.containsField("email") && obj.containsField("password")){
      val checkIns = obj.get("checkIns").asInstanceOf[BasicDBList].toList.asInstanceOf[List[DBObject]].map(
          (m: DBObject) => new CheckIn(m.get("location").asInstanceOf[String], 
        		  	  				  isoFormat.parse(m.get("createdOn").asInstanceOf[String]),
        		  	  				  isoFormat.parse(m.get("expiresOn").asInstanceOf[String]),
        		  	  				  m.get("validated").asInstanceOf[Boolean])
          )      
      return Some(new Account(Some(obj.get("_id").toString), 
    		  				  obj.get("email").toString,
    		  				  obj.get("password").toString,
    		  				  checkIns))
    }
    else{
      return None
    }
  }
    
  def login(account: Account):Option[Account] = account.asMap match {
    case Some(map) => retrieve(map) match {
      case Some(obj) => asModel(obj)
      case _ => None
    }
    case _ => None
  }
  
  override def reads(json: JsValue):JsResult[Account] =  JsSuccess(
    new Account(
      Some((json \ "id").as[String]),
      (json \ "email").as[String],
      (json \ "password").as[String],
      //TODO: Lookup parsing complex JSONs. Prototype is of course a prototype.
      List[CheckIn]()
      )
  )
  
  override def writes(account: Account):JsValue = {
	val _id:JsValue = account.id match {
	  case Some(id) => JsString(id)
	  case None => JsNull
	}
    return JsObject(List(
		  	"id" -> _id, 
		  	"email" -> JsString(account.email),
		  	"password" -> JsString(account.password)))
  }
  
  def checkIns(account: Account): JsValue = {
	 return JsArray(account.checkIns.map((checkIn) =>
	     JsObject(List(
	         "location" -> JsString(checkIn.locationId),
	         "createdOn" -> JsString(isoFormat.format(checkIn.createdOn)),
	         "expiresOn" -> JsString(isoFormat.format(checkIn.expiresOn)),
	         "validated" -> JsBoolean(checkIn.validated)
	         ))
	     ))
  }
} 
