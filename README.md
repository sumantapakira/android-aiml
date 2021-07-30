# AI Chatbot
AIML speech for Android 

Build your own chatbot using the library 

    <groupId>org.sumantapakira.aiml</groupId>
     <artifactId>android-aiml</artifactId>
    <version>1.0</version>

Lets first understand the syntax of each JSON key and how you can use it.

"pattern" is the input which you want to tell to the Bot

"template" is used to make the Bot understand how the response should be built. 

"voice" should be used when you want Bot to speak what it suppose to say

"context" is to make Bot understand, the conversation is about which topic. For example please see the "Tell me a joke" example https://github.com/sumantapakira/android-aiml/blob/main/example.json#L378-L395

"dependson" should be used when you want Bot to wait for some condition to fullfill. For example please see the coffee example https://github.com/sumantapakira/android-aiml/blob/main/example.json#L106-L139

"async" keyword is used when you want to perform network operation. For example, in the below example

         {
          "pattern" : "create a page",
          "template" : {
            "async" : {
              "api" : "http://192.168.178.29:4502/bin/servlets/createpage.json",
              "method" : "GET",
              "params" : [ "pagetitle", "pagelocation", "templatetype" ],
              "resultkey" : "path",
              "voice" : "Page is created "
            },
            "random" : [ "Sure, Please tell me the location, page title and template type", "What will be the location, template type and title" ]
          }
        }
        
you need to specify the endpoint in "api", "method" can be "GET/POST", "params" is what you want to send as request parameter to the endpoint and each value of params should be set before as below

        {
          "pattern" : "title is *",
          "template" : {
            "think" : {
              "dependson" : "create a page",
              "set" : {
                "name" : [ "pagetitle" ]
              }
            },
            "voice" : "ok, Page title is {pagetitle}<star/>"
          }
        }
        
Repeat can be used to get the last response from the Bot and this value you can store in variable for another purpose.

     "<repeat name='pagelocation'/>"  

"random" is where you can define random text which Bot should speak.
