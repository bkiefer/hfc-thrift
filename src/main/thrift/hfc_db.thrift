namespace java de.dfki.lt.hfc.db.remote
namespace py hfc_db
namespace cpp hfc_db

struct Table {
  1: required list<list<string>> rows;
}

struct QueryResult {
  1: required list<string> variables;

  2: required Table table;
}

struct PropInfo {
  1: required i32 type;

  2: required list<string> ranges;
}

exception QueryException {
  1: string why;
}

exception TupleException {
  1: string why;
}

exception WrongFormatException {
  1: string why;
}


service HfcDbService {
  // see if service is alive
  i32 ping(),

  // promote a namespace mapping to the server for further uploading of tuples
  bool addNamespace(1: string shortForm, 2: string longForm),

  // insert a set of tuples into the database
  // returns number of tuples inserted, or negative number in case of error
  i32 insertPlain(1: Table t) throws (1: TupleException tex),

  // remove a set of tuples from the db: don't know if we'll support that
  // i32 remove(1: Table t)

  // a select query that returns a result table
  // used by: DM, EM
  QueryResult selectQuery(1: string query) throws (1:QueryException qex),

  // a select query that returns a result table in string form
  string query(1: string query) throws (1:QueryException qex),

  // an ask query that returns a boolean
  bool askQuery(1: string query) throws (1:QueryException qex),

  // ///////// High-level API methods

  // insert a set of tuples into the db, adding to all tuples the current
  // timestamp
  // returns number of tuples inserted, or negative number in case of error
  i32 insert(1: Table t) throws (1: TupleException tex),

  // insert a set of tuples into the db, adding to all tuples the given
  // timestamp
  // returns number of tuples inserted, or negative number in case of error
  i32 insertTimed(1: Table t, 2: i64 timestamp) throws (1: TupleException tex),

  // Return a set of values of a non-functional property. Can also be applied
  // to a functional property, in which case the set can contain at most one
  // element
  set<string> getMultiValue(1: string uri, 2: string property)
              throws (1: TupleException tex, 2:QueryException qex),

  // Add a new value to a non-functional predicate
  // returns the number of tuples added to the repository
  i32 addToMultiValue(1: string uri, 2: string property, 3: string value)
      throws (1: TupleException tex),

  // Remove a value from a non-functional predicate.
  i32 removeFromMultiValue(1: string uri, 2: string property, 3: string value)
      throws (1: TupleException tex),

  // set the value a non-functional predicate to a set of values.
  i32 setMultiValue(1: string uri, 2: string property, 3: set<string> value)
      throws (1: TupleException tex),

  // return a new URI for an object of a given type in the given namespace
  // The namespace must end in a colon for short namespace specifications and
  // hash for long namespaces
  // the (timestamped) type statement is inserted by this method
  // used by: DM
  string getNewId(1: string nameSpace, 2: string type)
      throws (1: TupleException tex),

  // set the value of a functional property for the given uri
  // used by: DM
  i32 setValue(1: string uri, 2: string predicate, 3: string value)
      throws (1: TupleException tex),

  // get the value of a functional property for the given uri
  // used by: DM, EM
  string getValue(1: string uri, 2: string predicate)
         throws (1:QueryException qex),

  // method to support python client: get internal info about properties
  map<string, PropInfo> getAllProps(1: string classuri)
}
