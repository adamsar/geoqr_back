/*
 Base functions and shared Traits for models and data stores in
 GeoQR
 */

package models;

import com.mongodb.casbah.Imports._
import play.api.libs.json._
import java.text.SimpleDateFormat

object DataStore{
  //Overarching DB for interacting with collections
  val db = MongoClient("localhost", 27017)("testDBGeo")
  
  def col(name:String) = db(name)
}

//References an object that is stored in MongoDB 
//And retrievable as such
trait StoredInMongo{
  //Retrieves the article as an account
  def asMap:Option[scala.collection.mutable.Map[String, Any]];
}

/**
 * Object designed to archive things in DataStores
 */
trait DataWarehouse[A] extends Format[A]{
  //Collection name for the warehouse
  def colName:String;
  val col = DataStore.col(colName);
  
  //Standard iso 8601 format for dates
  val isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
  
  def asModel(obj: DBObject):Option[A]
  
  //Going with the analogy of a warehouse, archive an object 
  //for use later.
  def archive(obj: DBObject) = col.insert(obj)
  def archive(objectMap:scala.collection.mutable.Map[String, Any]):WriteResult = archive(objectMap.asDBObject)
  
  //Retrieves a single entity from the warehouse
  def retrieve(_id: ObjectId):Option[DBObject] = col.findOne(MongoDBObject("_id" -> _id))
  def retrieve(_id: String):Option[DBObject] = retrieve(new ObjectId(_id))
  def retrieve(criteria: DBObject):Option[DBObject] = col.findOne(criteria)
  def retrieve(criteria: scala.collection.mutable.Map[String, Any]):Option[DBObject] = col.findOne(criteria)
  
  //Search by criteria. Return raw results in order to apply limiter and what not later
  def search(criteria: DBObject):MongoCursor = col.find(criteria)
  def search(criteria: scala.collection.mutable.Map[String, Any]):MongoCursor = search(criteria.asDBObject)

  //Purge item from inventory
  def purge(_id: ObjectId):WriteResult = col.remove(MongoDBObject("_id" -> _id))
  def purge(_id: String):WriteResult = purge(new ObjectId(_id))
  
  def inventory = search(MongoDBObject())
}

//Helper for dealing with warehouse functions
object WarehouseHelpers{
  //Converts all entries in a cursor to a proper list
  def getModelList[A](cursor:MongoCursor, warehouse: DataWarehouse[A]):List[A] = 
    cursor.map(warehouse.asModel).
    collect({case Some(model) => model}).toList
}

//Queries and converts an inventory to a json array
class InventoryQuery[A]{
  def apply(warehouse:DataWarehouse[A]):JsValue = JsArray(
      WarehouseHelpers.getModelList[A](warehouse.inventory, warehouse).
      map(warehouse.writes)
      )
}
