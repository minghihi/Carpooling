package com.example.carpooling.api;

import com.example.carpooling.BookingRequest;
import com.example.carpooling.DriverBookingActionRequest;
import com.example.carpooling.models.RideResponse;
import com.example.carpooling.models.BookingResponse;
import com.example.carpooling.models.CreateRideRequest;
import com.example.carpooling.models.CreateRideResponse;
import com.example.carpooling.models.AuthResponse;
import com.example.carpooling.models.LoginRequest;
import com.example.carpooling.models.RegisterRequest;

import java.util.List;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {


    @POST("api/rides/create")
    Call<CreateRideResponse> createRide(@Body CreateRideRequest request);

    @POST("api/book")
    Call<ResponseBody> bookRide(@Body BookingRequest request);

    @GET("api/rides/available")
    Call<List<RideResponse>> getAvailableRides();

    @POST("api/driver/accept-booking")
    Call<ResponseBody> acceptBooking(@Body DriverBookingActionRequest request);

    @POST("api/driver/cancel-booking")
    Call<ResponseBody> cancelBooking(@Body DriverBookingActionRequest request);

    @GET("api/driver/pending-bookings")
    Call<List<BookingResponse>> getPendingBookings();

    @GET("api/user/my-bookings")
    Call<List<BookingResponse>> getMyBookings();

    @POST("api/user/cancel-booking")
    Call<ResponseBody> userCancelBooking(@Body BookingRequest request);

    @POST("api/auth/login")
    Call<AuthResponse> loginUser(@Body LoginRequest loginRequest);

    @POST("api/auth/register")
    Call<AuthResponse> registerUser(@Body RegisterRequest registerRequest);
}
