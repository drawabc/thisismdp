
#include "DualVNH5019MotorShield.h"  // https://github.com/pololu/dual-vnh5019-motor-shield
#include <Encoder.h>
#include "pins_arduino.h"
#include <EnableInterrupt.h>
//import the library in the sketch
#include <SharpIR.h>        // https://github.com/qub1750ul/Arduino_SharpIR  
#include "stdio.h" 
#include "math.h" 
//#include <PID_v1.h> // https://www.youtube.com/watch?v=crw0Hcc67RY //   https://github.com/br3ttb/Arduino-PID-Library
#include <RunningMedian.h>
//#include "PinChangeInt.h"

/*This is the PinChangeInt library for the Arduino. It provides an extension to
the interrupt support for ATmega328 and ATmega2560-based Arduinos, and some
ATmega32u4 and Sanguinos. It adds pin change interrupts, giving a way for users
to have interrupts drive off of any pin (ATmega328-based Arduinos), by the Port
B, J, and K pins on the Arduino Mega and its ilk, and on the appropriate ports
(including Port A) on the Sanguino and its ilk. */

DualVNH5019MotorShield md;
SharpIR sensor1( SharpIR::GP2Y0A41SK0F, A0 );  // S1
SharpIR sensor2( SharpIR::GP2Y0A41SK0F, A1 );  // S2   with sensing range of 10-80cm
SharpIR sensor3( SharpIR::GP2Y0A41SK0F, A2 );  // S3
SharpIR sensor4( SharpIR::GP2Y0A41SK0F, A3 );  // S4
SharpIR sensor5( SharpIR::GP2Y0A41SK0F, A4 );  // S5
SharpIR sensor6( SharpIR::GP2Y0A02YK0F, A5 );  // S6  with sensing range of 20-150cm

/*
int Sdistance1 = sensor1.getDistance(); //Calculate the distance in centimeters and store the value in a variable
int Sdistance2 = sensor2.getDistance();
int Sdistance3 = sensor3.getDistance();
int Sdistance4 = sensor4.getDistance();
int Sdistance5 = sensor5.getDistance();
int Sdistance6 = sensor6.getDistance();
*/

int step_counter = 0;
bool calibration_state = false;
bool calibration_angle = false;
bool calibration_dist = false;
bool fastest_path = false;

// RPM
const int encoderpin1a = 3; //encoder output for left motor M1 
const int encoderpin1b = 5; 

const int encoderpin2a = 11;//encoder output for right motor M2 
const int encoderpin2b = 13;

long ticksmoved1 = 0;
long ticksmoved2 = 0;

unsigned long currentpulsetime1=0;
unsigned long currentpulsetime2=0;

unsigned long previoustime1=0;
unsigned long previoustime2=0;

double integral;


// PID

#define PWM_MIN (-400)  // VNH5019 shield PWM min & max
#define PWM_MAX (400)
#define RATE 10 //ms, or 100Hz      // how quickly we read the angle and change the PWM

#define kp 2.0080
#define ki 0.0110
#define kd 0.0000

volatile long encoderLeftCounter = 0.000;
volatile long encoderRightCounter = 0.000;
long prevTick, prevMillis = 0;


// Moving speed.
#define Speed_Move 250

// Calibration speed
#define Speed_Calibration 250
#define Speed_Calibration_Angle 200

//Fastest path speed
#define Speed_Move_Fastest 385

//int setMotorSpeed = 50;

void stopIfFault()
{
  if (md.getM1Fault())
  {
    Serial.println("M1 fault");
    while(1);
  }
  if (md.getM2Fault())
  {
    Serial.println("M2 fault");
    while(1);
  }
}

void setup()
{
  Serial.begin(115200);
  
 // pinMode (encoderpin1a,INPUT);
 // pinMode (encoderpin1b,INPUT);
 // pinMode (encoderpin2a,INPUT);
  // pinMode (encoderpin2b,INPUT);
  
  enableInterrupt(encoderpin1a,encoder1change,RISING); // 3,
  enableInterrupt(encoderpin2a,encoder2change,RISING); // 11,
  
  md.init();
  
 }
 
void loop()
{
  forward(6);
}

double computePID() {
  double p, i, d, pid, error, integral;

  error = getRPM1() - getRPM2();
  Serial.println(error);
  
  integral += error;
  p = kp * error;
  i = ki * integral;
  d = kd * (prevTick - encoderLeftCounter);
  pid = p + i + d;

  return pid;
}

void encoder1change()
{
  if (digitalRead(encoderpin1b)==LOW)
  {
    encoderLeftCounter++;
  }
  else
  {
    encoderLeftCounter--;
  }
  currentpulsetime1=micros()-previoustime1;
  previoustime1=micros();

}

void encoder2change()
{
  if (digitalRead(encoderpin2b)==LOW)
  {
    encoderRightCounter++;
  }
  else
  {
    encoderRightCounter--;
  }
  currentpulsetime2=micros()-previoustime2;
  previoustime2=micros();
}

double getRPM1()
{
  if (currentpulsetime1==0)
  {
    return 0;
  }
  return 60000/(((currentpulsetime1)/1000.0)*562.215); // revolution per minute
}

double getRPM2()
{
  if (currentpulsetime2==0)
  {
    return 0;
  }
  return 60000/(((currentpulsetime2)/1000.0)*562.215);
}


void forward(double cm)
{

  double pid;
  /*
  
  int Set_Speed = (calibration_state == true) ? Speed_Calibration : Speed_Move;
  Set_Speed = (fastest_path == true) ? Speed_Move_Fastest : Set_Speed;
*/
int targetTick;
targetTick = cm * 29.65;

integral = 0;
int Set_Speed = Speed_Move;
encoderLeftCounter = encoderRightCounter = prevTick = 0;

        pid = computePID();       
        md.setSpeeds( (Set_Speed - pid),(Set_Speed + pid));
     //  md.setSpeeds( 300, 300 );
     delay(2000);
         Serial.println(getRPM2());
         Serial.println(getRPM1());

}
