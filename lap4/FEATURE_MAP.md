# Feature Map Lab 4

| Chức năng | Client/UI | Client protocol | Server |
|---|---|---|---|
| Khởi động server | script/server main | — | `MiniWebServer.main()` → `start()` |
| GET | action Go | `RawHttpClient.get()` | `ClientHandler.dispatch()` → `serveFile()` |
| HEAD | action HEAD | `RawHttpClient.head()` | `serveFile(request, false)` → `HttpResponse.writeTo(..., false)` |
| POST form | `MiniBrowser.sendPost()` | `RawHttpClient.postForm()` | `ClientHandler.handlePost()` |
| Parse request | — | — | `HttpRequest.parse()` |
| File tĩnh/media | render/download | parse body/header | `FileService.load()` → `MimeTypes` |
| Status/header | `displayResponse()` | `RawHttpClient.readResponse()` | `HttpResponse` |
| History | `refreshHistory()` | `BrowserHistory.add()` | — |
| Download | `downloadCurrentUrl()` | `RawHttpClient.get()` | GET file tĩnh |
| 404/405/500 | `showError()`/response view | parse status | `errorResponse()`, dispatch default |
| Chống traversal | request URL | target raw | `HttpRequest.validateAndGetPath()`, `FileService.load()` |

Khi JavaFX hóa, tên UI method có thể đổi nhưng protocol/server mapping phải giữ và bảng phải được cập nhật.
