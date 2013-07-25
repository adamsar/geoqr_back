package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import models.LocationWarehouse
import models.InventoryQuery
import models.Location
import models.WarehouseHelpers
import com.mongodb.casbah.commons.MongoDBObject


//All forms dealing with Locations
object LocationForms {
  val createForm = Form(
	tuple(
	    //TODO: write a formatter for float data types
        "code" -> text,
	    "lat" -> text,
	    "lon" -> text,
	    "info" -> text,
	    "timeToExpires" -> number
	    )
  )

  var indexForm = Form(
	tuple(
		"code" -> optional(text),
		"lat" -> optional(text),
		"lon" -> optional(text)
	)
  )
  
}

object Locations extends Controller{

  //Get a list of all accounts in the system
  def index = Action {implicit request =>
    LocationForms.indexForm.bindFromRequest.fold(failure => Ok("Failed"),
      {
      	case (Some(code), Some(lat), Some(lon)) => Ok(JsArray(
      	    WarehouseHelpers.getModelList[Location](
      	    	LocationWarehouse.search(
      	    	    MongoDBObject("code" -> code,
      	    	    			  "location" -> MongoDBObject(
      	    	    			      "$within" -> MongoDBObject(
      	    	    			    		 "$center" -> List(List(lat.toDouble, lon.toDouble), .5/111)
      	    	    			    		  )))), LocationWarehouse).map(LocationWarehouse.writes)))
      	case _ => Ok(((new InventoryQuery[Location])(LocationWarehouse)))
      }
    )    
  }

  //Create a new location to store stuff in
  def create = Action {implicit request =>
    LocationForms.createForm.bindFromRequest.fold(failure => Ok(failure.errors.map(_.message).toString),
        {
          case (code, lat, lon, info, timeToExpires) => {
            new Location(None, code, List(lat.toDouble, lon.toDouble), info, timeToExpires).asMap.map(LocationWarehouse.archive)
            Created("Success")
          }
        })
  }
  
  //Show a specific Location 
  def show(id: String) = Action {implicit request =>
    LocationWarehouse.retrieve(id) match {
      case Some(locationObj) => Ok(LocationWarehouse.writes(LocationWarehouse.asModel(locationObj).get))
      case None => NotFound("No such location found")
    }
  }
  
}
