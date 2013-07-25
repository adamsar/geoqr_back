package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import com.mongodb.casbah.Imports._
import models.LocationWarehouse
import models.AccountWarehouse
import java.util.Calendar
import models.CheckIn
import java.util.Date

//All forms dealing with CheckIns
object CheckInForms {
  //Check in to a location
  val createForm = Form(
	tuple(
          "lat" -> text,
          "lon" -> text,
          "code" -> text
        )
  )

  //Validate that the user has been checked into a location
  val validateForm = Form("location" -> text)
  
}

object CheckIns extends Controller{

  //Get a list of all CheckIns for a particular account
  def index(accountId:String) = Action {implicit request =>
    AccountWarehouse.retrieve(new ObjectId(accountId)) match {
      case Some(account) => AccountWarehouse.asModel(account) match {
        case Some(a) => Ok(AccountWarehouse.checkIns(a))
        case None => BadRequest("Error handling request")
      }
      case None => NotFound("No such account")
    }
  }

  //Create a new checkin for an account
  def create(accountId: String) = Action {implicit request =>
    CheckInForms.createForm.bindFromRequest.fold(
        failure => BadRequest("Unable to process request"),
        {
          case (lat, lon, code) => {
        	  val location = LocationWarehouse.retrieve(
        	    scala.collection.mutable.Map[String, Any](
        	      "location" -> Map(
        	          "$within" -> Map(
        	              "$center" -> List(List(lat.toDouble, lon.toDouble), .5/111)
        	              )
        	          ),
        	       "code" -> code
        	      )
        	  )
        	  location match {
        	    case Some(dbLoc) => {
        	      //Convert, get account, update checkins, return newest entry
        	      val convertedLocation = LocationWarehouse.asModel(dbLoc).get	      
        	      val associatedAccount = AccountWarehouse.retrieve(
        	          new ObjectId(accountId)
        	          ).map(AccountWarehouse.asModel).get
        	      associatedAccount match {
        	        case Some(account) => {
                      val cal = Calendar.getInstance
                      cal.add(Calendar.HOUR, convertedLocation.timeToExpires)
                      
                      //Check if they've already checked in here
                      if(AccountWarehouse.search(
                          MongoDBObject("_id" -> new ObjectId(account.id.get),
                    		  			"checkIns.location" -> convertedLocation.id.get)).isEmpty){
                    	  val checkIn = new CheckIn(convertedLocation.id.get,
                    			  					new Date,
                    			  					cal.getTime,
                    			  					false)
                    	  AccountWarehouse.col.update(MongoDBObject("_id" -> new ObjectId(account.id.get)),
                    			  					  $push("checkIns" -> checkIn.asMap.get.toMap.asDBObject))
                          Created("Checked In")
                      }else{
                        BadRequest("Already exists")
                      }
        	        }
                        case None => NotFound("No suitable account found to check in")
        	      }
        	    }
        	    case None => NotFound("No suitable locations found to check in")
        	  }
          }    
        })
  }

  def validate(account: String, locationId:String) = Action {implicit request =>
          AccountWarehouse.retrieve(new ObjectId(account)) match {
              case Some(acc)=> {
                AccountWarehouse.col.update(
                MongoDBObject("_id" -> acc.get("_id").asInstanceOf[ObjectId],
                              "checkIns.location" -> locationId),
                              $set("checkIns.$.validated" -> true))
                Ok("Updated")
              }
            case _ => NotFound("No such location")
        }
  }
}
