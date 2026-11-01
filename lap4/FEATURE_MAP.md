# Feature Map Lab 4

| Chức năng | Client/UI | Client protocol | Server |
|---|---|---|---|
| Khởi động server | script/server main | — | `MiniWebServer.main()` → `start()` |
| GET | `MiniBrowserApp.sendGet()` | `RawHttpClient.get()` | `ClientHandler.dispatch()` → `serveFile()` |
| HEAD | `MiniBrowserApp.sendHead()` | `RawHttpClient.head()` | `serveFile(request, false)` → `HttpResponse.writeTo(..., false)` |
| POST form | `MiniBrowserApp.sendPost()` | `RawHttpClient.postForm()` | `ClientHandler.handlePost()` |
| Parse request | — | — | `HttpRequest.parse()` |
| File tĩnh/media | render/download | parse body/header | `FileService.load()` → `MimeTypes` |
| Status/header | `MiniBrowserApp.displayResponse()` | `RawHttpClient.readResponse()` | `HttpResponse` |
| History | `MiniBrowserApp.addHistory()` | `BrowserHistory.add()` | — |
| Download | `MiniBrowserApp.downloadCurrentUrl()` | `RawHttpClient.get()` | GET file tĩnh |
| 404/405/500 | `showError()`/response view | parse status | `errorResponse()`, dispatch default |
| Chống traversal | request URL | target raw | `HttpRequest.validateAndGetPath()`, `FileService.load()` |

UI gọi request qua `MiniBrowserApp.runTask()`; automated flow nằm tại `HttpIntegrationTest.main()`.
