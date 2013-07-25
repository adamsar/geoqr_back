package controllers

import play.api._
import play.api.mvc._
import models.AccountWarehouse
import play.api.libs.json.JsArray
import play.api.data._
import play.api.data.Forms._
import models.Account
import models.InventoryQuery
import models.CheckIn
import com.mongodb.casbah.commons.MongoDBObject
import models.WarehouseHelpers

/**
 * Contains all the necessary forms for interacting with 
 * HTTP Verbs and the API
 */
object AccountForms{
  val createForm = Form(
      tuple(
          "email" -> text,
          "password" -> text
          )
      )
  val indexForm = Form(
      tuple(
          "email" -> optional(text),
          "password" -> optional(text)
          )
      )
}

object Accounts extends Controller{

  //Get a list of all accounts in the system
  def index = Action {implicit request =>
  	Ok((new InventoryQuery[Account])(AccountWarehouse))
  }
  
  def login = Action {implicit request =>
    AccountForms.indexForm.bindFromRequest.fold(
        failure => Ok("Shouldn't happen"),
        {
          case (Some(email), Some(password)) => 

              AccountWarehouse.retrieve(
                MongoDBObject("email" -> email,
            		      	  "password" -> password)) match {
                case Some(account) => Ok(AccountWarehouse.writes(AccountWarehouse.asModel(account).get))
                case _ => NotFound("Couldn't find this")
              }
          
          case _ => NotFound("Unspecified")
        })    
    
    }
  
  def create = Action {implicit request =>
  	AccountForms.createForm.bindFromRequest.fold(
  	    failure => Ok("Failed"),
  	    {
  	      case (email, password) => {
  	        new Account(None, email, password, List[CheckIn]()).asMap.map(AccountWarehouse.archive)
  	        Created("Success")
  	      }
  	    }) 
  }
}
