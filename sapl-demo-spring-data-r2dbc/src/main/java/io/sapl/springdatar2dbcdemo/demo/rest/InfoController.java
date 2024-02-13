package io.sapl.springdatar2dbcdemo.demo.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class InfoController {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @GetMapping("/info")
    public JsonNode info() throws JsonProcessingException {
        var firstFuncJsonString = """
                {
                  "method name based (1)": {
                    "name": "findAllByAgeAfter(int age)",
                    "query": "SELECT * FROM PERSON WHERE age > (:age)",
                    "manipulated query": "SELECT * FROM PERSON WHERE age > (:age) AND active = true",
                    "url": "http://localhost:8080/admin/findAllByAgeAfter/90",
                    "url with sapl": "http://localhost:8080/user/findAllByAgeAfter/90",
                    "policy": [
                      "policy \\"permit_general_protection_r2dbc_person_repository\\"",
                      "permit",
                      "where",
                      "     action == \\"general_protection\\"",
                      "obligation {",
                      "             \\"type\\": \\"r2dbcQueryManipulation\\"",
                      "             \\"conditions\\": [",
                      "                             \\"active = true\\"",
                      "                          ]",
                      "           }",
                      "obligation {",
                      "             \\"type\\": \\"filterJsonContent\\"",
                      "             \\"conditions\\": [",
                      "           {",
                      "             \\"type\\": \\"blacken\\",",
                      "             \\"path\\": \\"$.lastname\\",",
                      "             \\"discloseLeft\\": 2\\"",
                      "           }",
                      "           {",
                      "             \\"type\\": \\"delete\\",",
                      "             \\"path\\": \\"$.id\\"",
                      "           }",
                      "           ]",
                      "}"
                    ]
                  },
                  "query annotation based": {
                    "name": "fetchAllUsersWithQueryMethod(String lastnameContains)",
                    "query": "SELECT * FROM person WHERE lastname LIKE %(:lastnameContains)%",
                    "manipulated query": "SELECT * FROM person WHERE lastname LIKE %(:lastnameContains)% AND active = true",
                    "url": "http://localhost:8080/admin/fetchingByQueryMethodLastnameContains/ell",
                    "url with sapl": "http://localhost:8080/user/fetchingByQueryMethodLastnameContains/ell",
                    "policy": [
                      "policy \\"permit_general_protection_r2dbc_person_repository\\"",
                      "permit",
                      "where",
                      "     action == \\"general_protection\\"",
                      "obligation {",
                      "             \\"type\\": \\"r2dbcQueryManipulation\\"",
                      "             \\"conditions\\": [",
                      "                             \\"active = true\\"",
                      "                          ]",
                      "           }",
                      "obligation {",
                      "             \\"type\\": \\"filterJsonContent\\"",
                      "             \\"conditions\\": [",
                      "           {",
                      "             \\"type\\": \\"blacken\\",",
                      "             \\"path\\": \\"$.lastname\\",",
                      "             \\"discloseLeft\\": 2\\"",
                      "           }",
                      "           {",
                      "             \\"type\\": \\"delete\\",",
                      "             \\"path\\": \\"$.id\\"",
                      "           }",
                      "           ]",
                      "}"
                    ]
                  },
                  "custom method based": {
                    "name": "customRepositoryMethod",
                    "query": "custom defined queries cannot be manipulated. The objects from the database are filtered using the 'jsonContentFilterPredicate' handler.",
                    "filtering": "age >= 90 AND firstname regex \\"^.*er.*$\\"",
                    "url": "http://localhost:8080/admin/customRepositoryMethod",
                    "url with sapl": "http://localhost:8080/user/customRepositoryMethod",
                    "policy": [
                      "policy \\"permit_custom_repository_method_r2dbc_person_repository\\"",
                      "permit",
                      "where",
                      "     action == \\"custom_repository_method\\"",
                      "obligation {",
                      "             \\"type\\": \\"filterJsonContent\\"",
                      "             \\"conditions\\": [",
                      "           {",
                      "             \\"type\\": \\"blacken\\",",
                      "             \\"path\\": \\"$.lastname\\",",
                      "             \\"discloseLeft\\": 2\\"",
                      "           }",
                      "           {",
                      "             \\"type\\": \\"delete\\",",
                      "             \\"path\\": \\"$._id\\"",
                      "           }",
                      "           ]",
                      "}",
                      "obligation {",
                      "             \\"type\\": \\"jsonContentFilterPredicate\\"",
                      "             \\"conditions\\": [",
                      "           {",
                      "             \\"type\\": \\">=\\",",
                      "             \\"path\\": \\"$.age\\",",
                      "             \\"value\\": 90\\"",
                      "           }",
                      "           ]",
                      "}"
                    ]
                  },
                  "method name based (2)": {
                    "name": "findAllByAgeAfterAndActive(int age, boolean active)",
                    "query": "SELECT * FROM person WHERE age > (:age) AND active = (:active)",
                    "manipulated query": "SELECT * FROM person WHERE age > (:age) AND active = (:active) AND lastname LIKE '%ie%' ",
                    "url": "http://localhost:8080/admin/findAllByAgeAfterAndActive/18/false",
                    "url with sapl": "http://localhost:8080/user/findAllByAgeAfterAndActive/18/true",
                    "policy": [
                      "policy \\"permit_general_protection_reactive_user_repository\\"",
                      "permit",
                      "where",
                      "     action == \\"find_all_by_age\\"",
                      "obligation {",
                      "             \\"type\\": \\"mongoQueryManipulation\\"",
                      "             \\"conditions\\": [",
                      "                             \\"active = true\\"",
                      "                          ]",
                      "           }",
                      "obligation {",
                      "             \\"type\\": \\"filterJsonContent\\"",
                      "             \\"conditions\\": [",
                      "           {",
                      "             \\"type\\": \\"blacken\\",",
                      "             \\"path\\": \\"$.lastname\\",",
                      "             \\"discloseLeft\\": 2\\"",
                      "           }",
                      "           {",
                      "             \\"type\\": \\"delete\\",",
                      "             \\"path\\": \\"$.id\\"",
                      "           }",
                      "           ]",
                      "}"
                    ]
                  }
                }
                """;


        return MAPPER.readTree(firstFuncJsonString);
    }
}
