#ifndef HFCDBCLIENT_HPP__
#define HFCDBCLIENT_HPP__


#include <tecs-2/rpc/RPCFactory.hpp>


#include "../gen-cpp/HfcDbService.cpp"
#include "../gen-cpp/hfc_db_types.cpp"

namespace hfc_db{


class HfcDbClient{
public:
  HfcDbClient():_client(){}


  void init(std::string host, int port) {
    _client = de::dfki::tecs::rpc::RPCFactory::createSyncClient<HfcDbServiceClient>(host, port);
  }
  
  void shutdown() {
    // TODO how?
  }

  hfc_db::QueryResult query(std::string query) {
    hfc_db::QueryResult res;
    _client->selectQuery(res, query);
    return res;
  }


  std::vector<std::string> getAll(std::string predicate, std::string value){
    hfc_db::QueryResult r;
    
    _client->selectQuery(r,"select ?s ?ts where ?s "
            + predicate + " " + value + " ?ts "
            + "AGGREGATE ?ss = LGetLatest2 ?s ?ts \"1\"^^<xsd:int>");
    if (r.table.rows.size()==0)
      return std::vector<std::string>();
    return r.table.rows[0];
  }


  static std::vector<std::string> projectColumn(const Table& t, int col) {
    std::vector<std::string> result;
    for (const std::vector<std::string>& row : t.rows) {
      result.push_back(row[col]);
    }
    return result;
  }

   std::vector<std::string> getEvery(std::string predicate, std::string value) {
    QueryResult r;
    _client->selectQuery(r,"select ?s where ?s "
        + predicate + " " + value + " ?ts");
    return projectColumn(r.table, 0);
  }



  
  std::vector<std::string> getChildren() {
    return getEvery("<rdf:type>", "<dom:Child>");
  }

   std::vector<std::string> getAllProfessionals()  {
    return getEvery("<rdf:type>", "<dom:Professional>");
  }

   std::string createNewProessional()  {
     std::string res;
     
     _client->getNewId(res,"rifca:", "<dom:Professional>");
     return res;
  }

   std::string createNewChild() {
     std::string res;
     _client->getNewId(res,"rifca:", "<dom:Child>"); 
     return res;
     
   }

  
  /** Add new child id to given doctor
   * @throws TException
   */
   int treatsNewChild(std::string doctorUri, std::string childUri) {
    return _client->addToMultiValue(doctorUri, "<dom:treats>", childUri);
  }

  /** returns a new (unique) id (URI) for an object of type type
   * @throws TException
   */
  std::string getNewId(std::string _namespace, std::string type)  {
    std::string res;
    _client->getNewId(res,_namespace, type);
    return res;
  }

  hfc_db::Table getActiveGoals(std::string uri){
    hfc_db::QueryResult res;
    _client->selectQuery(res,  "select distinct ?agoal where "+uri+" <dom:hasActiveGoal> ?agoal ?t");
    return res.table;
  }
  std::string getValue(std::string instantiation, std::string dataProperty) {
    std::string res;
    _client->getValue(res,instantiation, dataProperty);
    return res;    
  }


private:
  std::unique_ptr<HfcDbServiceClient> _client;


};
}

#endif




