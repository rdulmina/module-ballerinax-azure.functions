// Copyright (c) 2023 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerinax/azure_functions as af;

public type Test record {
    string greet;
};

service / on new af:HttpListener() {
    resource function get err1(int|string a) returns string {
        return "done";
    }

    resource function get err2(int|string[]|float b) returns string {
        return "done";
    }

    resource function get err3(string? b, map<int> a) returns string {
        return "done";
    }

    resource function get err4(map<string>? c, map<json> d) returns string {
        return "done";
    }
    
    resource function get err5(int[]|json c) returns string {
        return "done";
    }

    resource function get err6(map<int>[]?  c) returns string {
        return "done";
    }

    resource function get err7(map<json>[]?  c) returns string {
        return "done";
    }
}
