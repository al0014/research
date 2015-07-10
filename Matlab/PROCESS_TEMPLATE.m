%%initialization and read

clc;
clear;
close all;

load('Ctrl_Channel.mat');
load('RAW signal.mat');

%% read section
%signalx=Subject2_Power_Beta(2,500:end)';
signalx=Ctrl_Channel(2,500:end)';
rawdata=Channel(2:end,:);
Pow_avg=0;% redefined later

%% plot section
figure (1);
subplot(211);
plot(linspace(0,length(signalx)/128,length(signalx)),signalx);
xlabel('TimeSeries/Sec');
subplot(212);
plot(linspace(0,60,length(signalx)),fft(signalx));
xlabel('Frequency/Hz');
ylabel('Power');
title('bipolar control signal frequency');
grid on;
%figure (2);
%subplot(16,1,1);
plot(linspace(0,length(Subject2_Power_Beta)/256,length(Subject2_Power_Beta)),Subject2_Power_Beta(2,:));

