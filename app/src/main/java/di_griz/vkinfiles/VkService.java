package di_griz.vkinfiles;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface VkService {
    @GET("/method/messages.delete")
    Call<DeleteResponse> delete(@Query("message_ids") String message_ids,
                                @Query("delete_for_all") int delete_for_all,
                                @Query("access_token") String access_token,
                                @Query("v") String v);

    @GET("/method/messages.getById")
    Call<GetByIdResponse> getById(@Query("message_ids") String message_ids,
                                  @Query("access_token") String access_token,
                                  @Query("v") String v);

    @GET("/method/messages.getHistory")
    Call<GetHistoryResponse> getHistory(@Query("user_id") int user_id,
                                        @Query("count") int count, @Query("offset") int offset,
                                        @Query("access_token") String access_token,
                                        @Query("v") String v);

    @GET("/method/messages.send")
    Call<SendResponse> send(@Query("user_id") int user_id,
                            @Query("attachment") String attachment,
                            @Query("access_token") String access_token,
                            @Query("v") String v);

}
