package di_griz.vkinfiles;

import java.util.List;

class GetHistoryResponse {
    class Response {
        int count;
        List<Message> items;
    }

    Response response;
}
