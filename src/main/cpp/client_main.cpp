#include <iostream>
#include <fstream>
#include <string>


#include "HfcDbClient.hpp"

int main (int argc, char *argv[]){
  hfc_db::HfcDbClient client;
  client.init("127.0.0.1",9090);
  std::vector<std::string> children=client.getEvery("<rdf:type>","<dom:Child>");
  for(auto child :children){
    std::cout<<child<<std::endl;
    for(auto row :client.getActiveGoals(child).rows)
    for(auto goal :row)
      if(std::string::npos!=client.getValue(goal, "<rdf:type>").find("<goal:Answer"))
	{
	  std::string res=client.getValue(goal, "<rdf:type>");
	  assert(res.length()>14);
	  res=res.substr(12,res.length()-13);
	  std::cout<<res<<std::endl;
	}

  }
  return 0;
}

