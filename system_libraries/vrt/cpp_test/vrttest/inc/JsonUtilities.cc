/* ===================== COPYRIGHT NOTICE =====================
 * This file is protected by Copyright. Please refer to the COPYRIGHT file
 * distributed with this source distribution.
 *
 * This file is part of REDHAWK.
 *
 * REDHAWK is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * REDHAWK is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 * ============================================================
 */

#include "JsonUtilities.h"
#include "HasFields.h"
#include "Utilities.h"
#include <queue>
#include <stdlib.h>   // for atof(..)

using namespace std;
using namespace vrt;
using namespace vrttest;

////////////////////////////////////////////////////////////////////////////////
// CONVERT VALUE TO JSON
////////////////////////////////////////////////////////////////////////////////
/** Converts a Value object to a JSON object with indentation as listed. */
static string toJSON (const string &indent, Value *obj) {
  string indent2 = indent + "  ";   // indent+2
  string indent4 = indent + "    "; // indent+4

  if ((obj == NULL) || isNull(*obj)) {
    return "null";
  }

  switch (obj->getType()) {
    case ValueType_Int8:      return obj->toString();
    case ValueType_Int16:     return obj->toString();
    case ValueType_Int32:     return obj->toString();
    case ValueType_Int64:     return obj->toString();
    case ValueType_Float:     return obj->toString();
    case ValueType_Double:    return obj->toString();
    case ValueType_Bool:      return (obj->as<bool>())? "true" : "false";
    case ValueType_BoolNull:  return (obj->as<bool>())? "true" : "false";
    case ValueType_String:    return string("\"") + obj->toString() + "\"";
    case ValueType_WString:   return string("\"") + obj->toString() + "\"";
    case ValueType_VRTObject:
      try {
        HasFields *hf = obj->as<HasFields*>();
        string json  = "{";

        for (int32_t i = 0; i < hf->getFieldCount(); i++) {
          if (i > 0) json += ",";

          string key = hf->getFieldName(i);
          string val = toJSON(indent4, hf->getField(i));

          json += "\"" + key + "\" : " + val;
        }
        json += "\n" +indent + "}";

      }
      catch (VRTException e) {
        UNUSED_VARIABLE(e);
        return string("\"") + obj->toString() + "\"";
      }
    default: // Must be vector
      string json  = "[";

      for (size_t i = 0; i < obj->size(); i++) {
        if (i > 0) json += ",";

        json += "\n" + indent2;
        Value *v = obj->at(i);
        json += toJSON(indent4, v);
        delete v;
      }
      json += "\n" +indent + "]";
      return json;
  }
}

string JsonUtilities::toJSON (Value *obj) {
  return ::toJSON("", obj);
}

////////////////////////////////////////////////////////////////////////////////
// CONVERT JSON TO VALUE
////////////////////////////////////////////////////////////////////////////////

/** Indicates if c is a JSON whitespace character. */
static bool isWhitespace (char c) {
  return (c == ' ') || (c == '\t')  || (c == '\n')
      || (c == '\r');
}

/** Indicates if c is a JSON operator. */
static bool isOperator (char c) {
  return (c == ':') || (c == '[') || (c == '{')
      || (c == ',') || (c == ']') || (c == '}');
}

/** Indicates if s is a JSON operator. */
static bool isOperator (const string &s) {
  return (s.length() == 1) && isOperator(s[0]);
}

/** Tokenizes a JSON input string. */
static queue<string> tokenize (const string &json) {
  queue<string> tokens;

  size_t  length = json.length();
  int32_t start  = -1;
  for (size_t i = 0; i < length; i++) {
    char c = json[i];

    if (isWhitespace(c)) {
      if (start >= 0) {
        // End of token
        tokens.push(json.substr(start,i-start));
      }
      start = -1;
    }
    else if (isOperator(c)) {
      if (start >= 0) {
        // End of token
        tokens.push(json.substr(start,i-start));
      }
      // Plus operator token
      tokens.push(json.substr(i,1));
      start = -1;
    }
    else if (start >= 0) {
      // Still in current token
    }
    else if (c == '\"') {
      // New string token
      start = (int32_t)i;
      while ((start >= 0) && (i < length)) {
        i++;
        c = json[i];

        if (c == '\"') {
          // End of string
          tokens.push(json.substr(start,i-start+1));
          start = -1;
        }
        else if (c == '\\') {
          // Escape sequence
          if (i == length-1) {
            throw VRTException("Invalid JSON object, expected escape after '\\'");
          }
          i++;
        }
      }
      if (start >= 0) throw VRTException("Invalid JSON object, expected '\"'");
    }
    else {
      // New non-string token
      start = (int32_t)i;
    }
  }
  if (start >= 0) {
    tokens.push(json.substr(start,length-start));
  }
  return tokens;
}

/** Converts JSON string to Value string. */
static string _fromStr (const string &token) {
  if ((token[0] != '\"') || (token[token.size()-1] != '\"')) {
    throw VRTException("Invalid JSON string, missing quote near '%s'.", token.c_str());
  }
  string t = token.substr(1, token.length()-2);
  size_t i;
  while ((i = t.find("\\\"")) != string::npos) {
    t = t.substr(0,i) + "\"" + t.substr(i+2);
  }
  return t;
}

/** Converts a single-entry JSON object to a Java object. */
static Value* _fromJSON (const string &token) {
  if (token == "null"  ) return new Value();
  if (token == "true"  ) return new Value(true);
  if (token == "false" ) return new Value(false);
  if (token.size() == 0) throw VRTException("Invalid JSON object, expected null, Boolean, "
                                            "String, or Number, but found ''.");
  if (token[0] == '\"' ) return new Value(_fromStr(token));

  if (!isOperator(token)) {
    return new Value(atof(token.c_str()));
  }

  throw VRTException("Invalid JSON object, expected null, Boolean, "
                     "String, or Number, but found '%s'.", token.c_str());
}

/** Polls the queue returning "" if empty. */
static string poll (queue<string> &tokens) {
  if (tokens.empty()) return "";
  string str = tokens.front();
  tokens.pop();
  return str;
}

/** Converts a JSON object to a Java object.
 *  @param json The JSON object to convert.
 *  @return The Java object.
 */
static Value* fromJSON (string first, queue<string> &tokens) {
  // Get first entry (if not already present)
  if (isNull(first)) first = poll(tokens);

  if (isNull(first)) {
    throw VRTException("Premature end of JSON object/array");
  }
  else if (first == "{") {
    map<string,Value*> _map;
    try {
      while (true) {
        first = poll(tokens);

        // Preliminary checks
             if (isNull(first)   ) throw VRTException("Invalid JSON object, expected '}'");
        else if (first == "}"    ) break; // done
        else if (_map.size() == 0) { } // no comma expected
        else if (first == ","    ) first = poll(tokens); // found comma, now check next entry
        else                       throw VRTException("Invalid JSON object, expected '}' or ','");

        // Secondary checks
        string sep = poll(tokens);
        string val = poll(tokens);
        if (isNull(first)    ) throw VRTException("Invalid JSON object, expected key");
        if (isOperator(first)) throw VRTException("Invalid JSON object, expected key before '%s'", first.c_str());
        if (isNull(sep)      ) throw VRTException("Invalid JSON object, expected ':' after %s", first.c_str());
        if (sep != ":"       ) throw VRTException("Invalid JSON object, expected ':' after %s", first.c_str());
        if (isNull(val)      ) throw VRTException("Invalid JSON object, expected value for %s", first.c_str());

        _map[_fromStr(first)] = fromJSON(val,tokens); // key must be a string
      }
    }
    catch (VRTException e) {
      for (map<string,Value*>::iterator it = _map.begin(); it != _map.end(); ++it) {
        delete it->second;
      }
      throw e;
    }
    return new Value(_map, true);
  }
  else if (first == "[") {
    vector<Value*> *list = new vector<Value*>();
    try {
      while (true) {
        first = poll(tokens);

        // Preliminary checks
             if (isNull(first)    ) throw VRTException("Invalid JSON array, expected ']'");
        else if (first == "]"     ) break; // done
        else if (list->size() == 0) { } // no comma expected
        else if (first == ","     ) first = poll(tokens); // found comma, now check next entry
        else                        throw VRTException("Invalid JSON array, expected '}' or ','");

        // Secondary checks
        if (isNull(first)    ) throw VRTException("Invalid JSON array, expected value");
        if (isOperator(first)) throw VRTException("Invalid JSON array, expected value before '%s'", first.c_str());

        list->push_back(fromJSON(first, tokens));
      }
    }
    catch (VRTException e) {
      for (size_t i = 0; i < list->size(); i++) {
        delete list->at(i);
      }
      safe_delete(list);
      throw e;
    }
    return new Value(list, true);
  }
  else {
    // single entry
    return _fromJSON(first);
  }
}

Value* JsonUtilities::fromJSON (const string &json) {
  string str = Utilities::trim(json);
  queue<string> tokens = tokenize(str);
  return ::fromJSON("", tokens);
}
