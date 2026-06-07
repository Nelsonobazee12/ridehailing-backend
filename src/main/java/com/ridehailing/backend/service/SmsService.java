package com.ridehailing.backend.service;

public interface SmsService {
    void sendSms(String phoneNumber, String message);
    void notifyTripAccepted(String riderPhone, String riderName,
                            String driverName, String plateNumber);
    void notifyDriverTripAccepted(String driverPhone, String riderName,
                                  String pickupAddress);
    void notifyDriverArrived(String riderPhone, String driverName,
                             String plateNumber);
    void notifyTripStarted(String riderPhone, String destinationAddress);
    void notifyTripCompleted(String riderPhone, String riderName,
                             Double fare, Double distanceKm);
    void notifyDriverTripCompleted(String driverPhone, String driverName,
                                   Double earnings);
    void notifyTripCancelled(String phone, String name, String reason);
}