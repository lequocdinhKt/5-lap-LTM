# Feature Map Lab 4

| Chức năng | Client/UI | Service/protocol | Server |
|---|---|---|---|
| Khởi động server | script/server main | — | `MiniWebServer.main()` → `start()` |
| GET | `MiniBrowserApp.sendGet()` | `BrowserService.get()` → `RawHttpClient.get()` | `ClientHandler.dispatch()` → `serveFile()` |
| HEAD | `MiniBrowserApp.sendHead()` | `BrowserService.head()` → `RawHttpClient.head()` | `serveFile(request, false)` → `HttpResponse.writeTo(..., false)` |
| POST form | `MiniBrowserApp.sendPost()` | `BrowserService.post()` → `RawHttpClient.postForm()` | `ClientHandler.handlePost()` |
| Parse request | — | — | `HttpRequest.parse()` |
| File tĩnh/media | render/download | parse body/header | `FileService.load()` → `HttpResponse.file()` → `InputStream.transferTo()` |
| Status/header | `MiniBrowserApp.displayResponse()` | `BrowserService.PageResult` ← `RawHttpClient.readResponse()` | `HttpResponse` |
| History | `MiniBrowserApp.refreshHistory()` | `BrowserService` → `BrowserHistory.add()` | — |
| Download | `MiniBrowserApp.downloadCurrentUrl()` | `BrowserService.download()` → `RawHttpClient.get()` | GET file tĩnh |
| 404/405/500 | `showError()`/response view | parse status | `errorResponse()`, dispatch default |
| Chống traversal | request URL | target raw | `HttpRequest.validateAndGetPath()`, `FileService.load()` |

UI gọi `BrowserService` qua `MiniBrowserApp.runTask()`; automated flow nằm tại `HttpIntegrationTest.main()`.
