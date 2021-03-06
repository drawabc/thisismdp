   //import Motor library in the sketch
  #include "DualVNH5019MotorShield.h"  // https://github.com/pololu/dual-vnh5019-motor-shield
  #include <EnableInterrupt.h>   // Rising edge of encoder 
  
  //Import Library for Sensors
  //#include <ResponsiveAnalogRead.h>
  #include <SharpIR.h>        // https://github.com/qub1750ul/Arduino_SharpIR  
  #include <math.h>
  
  DualVNH5019MotorShield md; // motor object
  
  /*
   * 
  Pololu motor encoders generate 48 �ticks� or �counts� per rotation of the motor shaft
  ? Each full rotation of the ~47:1 gearbox output shaft
  = 47.851 rotations of the motor shaft
  = 2249 ticks per wheel rotation
  
   562.25 square waves for every revolution of wheel.alg
  
  Command list
  1 - Forward 
  2 - Turn Left 
  3 - Turn Right
  4 - Backward
  5 - Start Exploration
  6 - Start Faster Path
  7 - (force ) End
  8 - Calibrate ( Check Sensors )
  9- Send Way Point Coordinates
  
  Baud rate - 115200 
  
  wheelRadius 5.75  cm
  #define distanceBtwWheel 19.0  cm
  */
  
  int step_counter = 0;
  bool calibration_state = false;
  bool calibration_angle = false;
  bool calibration_dist = false;
  bool fastest_path = false;
  
  // RPM
  const int encoderpin1a = 3; //Digital encoder output for left motor M1 into Arduino  
  const int encoderpin1b = 5; 
  
  const int encoderpin2a = 11;//encoder output for right motor M2 
  const int encoderpin2b = 13;
  
  long ticksmoved1 = 0;
  long ticksmoved2 = 0;
  
  unsigned long currentpulsetime1=0;
  unsigned long currentpulsetime2=0;
  
  unsigned long previoustime1=0;
  unsigned long previoustime2=0;
  
  
  volatile long encoderLeftCounter;
  volatile long encoderRightCounter;
  
  long prevTick = 0;
  long prevMillis = 0;
  
  
  double integral;
  
  // ALIGN PARAMETERS
  # define RIGHT_ERR_THRES 1.0
  # define RIGHT_SENSOR_DIST 20
  # define RIGHT_CLOSELIMIT 10.0
  # define RIGHT_FARLIMIT 13.0
  # define FRONT_ERR_THRES 1.0
  
  // PID
  
  #define kp 0.80000//5
  #define ki 0.0009
  #define kd 0.0000
  
  
  
  // Moving speed.
  // TODO: change and recalibrate speed
  #define Speed_Move 300
  #define motor_offset 21.00
  
  // Turning speed
  #define Speed_Spin 250
  #define Rotate_Spin 100
  
  //Speed_Move = 100, motor_offset = 9
  //Speed_Move = 150, motor_offset = 11
  //Speed_Move = 200, motor_offset = 13
  //Speed_Move = 250 , motor_offset = 18
  //Speed_Move = 300 , motor_offset = 21
  //Speed_Move = 350 , motor_offset = 23

  
  // Brake speed
  #define Speed_Brake 360
  #define Speed_Brake_Align 100
  
  /*---------------Sensors-------------------*/
  
  #define irFM A0 //DEFINE signal pins
  #define irFL A1
  #define irFR A2
  #define irLF A3
  #define irRB A4
  #define irRF A5
  
  //SensorName(modelNumber, pin)
  //frontIR_2
  SharpIR sensorFM(SharpIR::GP2Y0A21YK0F, A0 );
  // leftIR_1
  SharpIR sensorLF(SharpIR::GP2Y0A02YK0F, A3 );
  // rightIR_1
  SharpIR sensorRF(SharpIR::GP2Y0A21YK0F, A5 );
  //frontIR_1
  SharpIR sensorFL(SharpIR::GP2Y0A21YK0F, A1 );
  // rightIR_2
  SharpIR sensorRB(SharpIR::GP2Y0A21YK0F, A4 );
  //frontIR_3
  SharpIR sensorFR(SharpIR::GP2Y0A21YK0F, A2 ); 
  
  /*CODE  
   *GP2Y0A41SK0F = 0 ; 
   *GP2Y0A21YK0F = 1 ; 10cm to 80cm (>80cm: returns 81 && <10cm: returns 9)
   *GP2Y0A02YK0F = 3 ; 20cm t0 150cm (>150cm: returns 151 && <20cm: returns 19)
  
  */
  
  double distFL = 0.0, distFM = 0.0, distFR = 0.0, distRB = 0.0, distRF = 0.0, distLF = 0.0;
  
  
  double final_MedianRead(int tpin) {
    double sum = 0;
    double x[31];  
    for (int i = 0; i < 31; i ++) {
      x[i] = distanceFinder(tpin);
    }
    insertionsort(x, 31);
    return x[15];
  }
  
  //get distance in CM
  double distanceFinder(int pin)
  {
    double dis = 0.00;
    
    switch (pin)
    {
      //raw dist + offset from centre
      case irFM:
        dis = sensorFM.getDistance()+1; 
        break;
      case irLF:
        dis = sensorLF.getDistance();
        break;
      case irRF:
        dis = sensorRF.getDistance();
        break;
      case irFL:
        dis = sensorFL.getDistance(); 
        break;
      case irRB:
        dis = sensorRB.getDistance();
        break;
      case irFR:
        dis = sensorFR.getDistance();
        break;
      default:
        break;
    }
    return dis;
  }
  
  void insertionsort(double array[], int length)
  {
    double temp;
    for (int i = 1; i < length; i++) {
      for (int j = i; j > 0; j--) {
        if (array[j] < array[j - 1])
        {
          temp = array[j];
          array[j] = array[j - 1];
          array[j - 1] = temp;
        }
        else break;
      }
    }
  }
  
  //DISTANCE in GRIDS NEED TO change range
  double longdistanceInGrids(double dis){
    int grids ;
  
    if (dis >= 19.0 && dis <= 19.9){
        grids = 1;
    }
    else if (dis >= 20.0 && dis <= 26.8){
        grids = 2;
    }
    else if (dis >= 26.9  && dis <= 30){
        grids = 3;
    }
    else if (dis >= 31.00 && dis <= 37.00){
        grids = 4;
    }
    /*else  if (dis >= 35.0 && dis <= 39){
        grids = 5;
    }
    else if ( dis> 39.0 && dis<=42) {
      grids = 6;
    }*/
    else 
      grids = 9; // out of range
        
    return grids;
    
  }
  
  double distanceInGrids(double dis){
    
    int grids ;
  
    if (dis>= 7.0 && dis < 16)
        grids = 1;
    else if (dis >= 16 && dis < 24)
        grids = 2;
    /*else if (dis >= 24.0 && dis < 33.1)
        grids = 3;
    else if (dis >= 33.1  && dis <= 40.0)
        grids = 4;
    else if (dis >= 40.1  && dis <= 45.1)
        grids = 5;*/
    else 
        grids = 9; // not accurate
    return grids;
  }
  
  void flush_SensorData() {  // Flush Sensor Values
    distFL = 0.0; distFM = 0.0; distFR = 0.0; distRB = 0.0; distRF = 0.0; distLF = 0.0;
  }
  
  /**
   * 
   * 
  Sensor Data (refer to enum for exact Command)
  
  1. Ard|Alg|Sensor|1:RawData:Block,2:RawData:Block,3:RawData:Block,4:RawData:Block,5:RawData:Block,6:RawData:Block
  (First number corresponds to sensor number) 
  
  1 � Front Sensor (Left)
  2 � Front Sensor (Center)
  3 � Front Sensor (Right)
  4 � Right Sensor (Top)
  5 � Right Sensor (Below)
  6 � Left Sensor
  
   * 
   */
  /*
  void Export_Sensors_CM() {  // Print Sensor Data
    
    String resultFM = String("  FM: ") + String(final_MedianRead(irFM)); // 2 
    String resultLF = String("  LF: ") + String(final_MedianRead(irLF));  // 6
    String resultRF = String("  RF: ") + String(final_MedianRead(irRF));  // 4  <top>
    String resultFL = String("  FL: ") + String(final_MedianRead(irFL)); // 1
    String resultRB = String("  RB: ") + String(final_MedianRead(irRB)); // 5 < below>
    String resultFR = String("  FR: ") + String(final_MedianRead(irFR)); // 3
    Serial.println("front: "+ resultFM + resultFL + resultFR );
    Serial.println("right: " + resultRF + resultRB);
    Serial.println("left: "+ resultLF);
    Serial.println("==========================================");
  
  }
    Serial Comm
    */
  /*
  //--------------------------Serial Codes-------------------------------
  void setupSerialConnection() {
    Serial.begin(115200);
    while (!Serial);
  }
  */
  
  void returnSensorReading_Raw() {
    
    double gridsFL = final_MedianRead(irFL);
    double gridsFM = final_MedianRead(irFM);
    double gridsFR = final_MedianRead(irFR);
    double gridsRF = final_MedianRead(irRF);
    double gridsRB = final_MedianRead(irRB);
    double gridsLF = final_MedianRead(irLF);
    
    Serial.print("Ard|Alg|S|1:");
    Serial.print( String(gridsFL)); // Raw Data 1
    Serial.print(":");
    Serial.print(distanceInGrids(gridsFL)); // block 
    Serial.print(",2:");
    Serial.print( String(gridsFM) );   // Raw data 2
    Serial.print(":");
    Serial.print(distanceInGrids(gridsFM));
    Serial.print(",3:");
    Serial.print( String(gridsFR) );  // Raw data 3
    Serial.print(":");
    Serial.print(distanceInGrids(gridsFR));
    Serial.print(",4:");
    Serial.print( String(gridsRF));   // Raw data 4
    Serial.print(":");
    Serial.print(distanceInGrids(gridsRF));
    Serial.print(",5:");
    Serial.print( String(gridsRB));    // Raw data 5
    Serial.print(":");
    Serial.print(distanceInGrids(gridsRB));
    Serial.print(",6:");
    Serial.print( String(gridsLF));    // Raw data 6
    Serial.print(":");
    Serial.println(longdistanceInGrids(gridsLF));
    Serial.flush();
  }
  
  
  double computePID() {
    double p, i, d, pid, error, integral;
  
    error = getRPM1() - getRPM2();
    // Serial.println("PID Error " + String (error));
    
    integral += error;
    p = kp * error;
    i = ki * integral;
    d = kd * (prevTick - encoderLeftCounter);
    pid = p + i + d;
  
    return pid;
  }
  
  void encoder1change()
  {
    if (digitalRead(encoderpin1b) == LOW){
      encoderLeftCounter++;
    }
    else{
      encoderLeftCounter--;
    }
    
    currentpulsetime1=micros()-previoustime1;
    previoustime1=micros();
  
  }
  
  void encoder2change()
  {
  
    if (digitalRead(encoderpin1b) == LOW){
      encoderRightCounter++;
    }
    else{
      encoderRightCounter--;
    }
    
    encoderRightCounter++;
    currentpulsetime2=micros()-previoustime2;
    previoustime2=micros();
  }
  
  double getRPM1()
  {
    if (currentpulsetime1==0){
      return 0;
    }
    return 60000/(((currentpulsetime1)/1000.0)*562.215); // revolution per minute
  }
  
  double getRPM2()
  {
    if (currentpulsetime2==0){
      return 0;
    }
    return 60000/(((currentpulsetime2)/1000.0)*562.215);
  }
  
  void resetEncoder() {
    encoderRightCounter = 0.0;
    encoderLeftCounter = 0.0;
  }
  
  void Forward(double cm){
    double pid;
    int targetTick;
  
    int Set_Speed = Speed_Move; //150
    int Set_Speed2 = 100;
    int motor_offset1 = 9;
    encoderLeftCounter = 0 ;
    encoderRightCounter = 0 ;
    prevTick = 0 ;
    integral = 1;
    targetTick = cm * 29.65;
    int Set_Speed1 = Set_Speed - motor_offset;
    // wheelcircumference = 2.0 * PI * (wheelRadius / 2);  // 18.06
    //distance travelled = ( abs(encoderRightCounter / 526.25 ) * wheelcircumference)       (rising edge of  every revolution of wheel) - 526.25 
  
    // PI * wheel Radius  = wheelcircumeference 
    
    // Move Forward 1 grid
    if (cm ==1){
      targetTick = cm *  27.3;
      while( encoderLeftCounter < targetTick){ //encoderLeftCounter < targetTick  
         pid = computePID();
        // md.setSpeeds( (50 - pid),(50 + pid));
        md.setSpeeds( ( (Set_Speed2 - motor_offset1) - pid),(Set_Speed2 + pid) );
        }
        md.setBrakes(350, 350);
    }
    else{
      if ( cm <= 10 ) {     
        targetTick = cm * 26.3;// 27.3 for 1 grid
        while(encoderLeftCounter < targetTick){ //encoderLeftCounter < targetTick  
           pid = computePID();
           md.setSpeeds( (Set_Speed1 - pid),(Set_Speed + pid));  // (motor1, motor2) motor 1 will be faster, need to - with motor_offset
        }
      }
      else if ( cm <= 30 ) { 
        targetTick = cm * 29.1; 
        while (encoderLeftCounter < targetTick) {
      
           pid = computePID();
           md.setSpeeds( (Set_Speed1- pid ),(Set_Speed + pid));  // (motor1, motor2) motor 1 will be faster, need to - with motor_offset
           //Serial.println((String)getRPM1());
           //Serial.println((String)getRPM2());
           //Serial.println((String)encoderLeftCounter);
        }
        // Move Forward 5 grid     
      } 
      else if ( cm <= 50 ) { 
        targetTick = cm * 29.75;
        while (encoderLeftCounter < targetTick) {
           pid = computePID();
           md.setSpeeds( ( Set_Speed1 - pid),(Set_Speed + pid));  // (motor1, motor2) motor 1 will be faster, need to - with motor_offset
           //Serial.println((String)getRPM1());
           //Serial.println((String)getRPM2());
            //Serial.println((String)encoderLeftCounter);
        }
     
       // Move Forward 6 grid
      } 
      else if ( cm <= 60 ) {   
        targetTick = cm * 29.8163;
        while (encoderLeftCounter < targetTick) {  
           pid = computePID();
           md.setSpeeds( ( Set_Speed1 - pid ),(Set_Speed + pid));  // (motor1, motor2) motor 1 will be faster, need to - with motor_offset
           //Serial.println((String)getRPM1());
           //Serial.println((String)getRPM2());
           //Serial.println((String)encoderLeftCounter);
        }
      } 
      else if ( cm <= 150 ) {   
       targetTick = cm * 29.8163;
       while (encoderLeftCounter < targetTick) {
           pid = computePID();
           md.setSpeeds( (Set_Speed1 - pid ),(Set_Speed + pid));  // (motor1, motor2) motor 1 will be faster, need to - with motor_offset
            //Serial.println((String)getRPM1());
            //Serial.println((String)getRPM2());
            // Serial.println((String)encoderLeftCounter);
       }
      }
      else {
        // move Forward 1 grid
        while (encoderLeftCounter < targetTick) {
           pid = computePID();
           md.setSpeeds( ( Set_Speed1 - pid ),(Set_Speed + pid));  // (motor1, motor2) motor 1 will be faster, need to - with motor_offset
          //Serial.println((String)getRPM1());
          //Serial.println((String)getRPM2());
          //Serial.println((String)encoderLeftCounter);
        }
      }
      md.setBrakes(Speed_Brake, Speed_Brake);
    }
    //TODO: DECELERATE
  }
  
  void Reverse(double cm) {
  
    double pid;
    int targetTick;
    int Set_Speed = 100; // Speed_Move; 
    int motor_offset1 = 9;
  
    encoderLeftCounter = 0 ;
    encoderRightCounter = 0 ;
    prevTick = 0 ;
    integral = 0;
    
    targetTick = cm * 29.65;
  
    while (-encoderLeftCounter < min(50, targetTick)) {  // min() Returns The smaller of the two numbers. 
      pid = computePID();
      md.setSpeeds( -((Set_Speed - pid)- motor_offset1 ),-(Set_Speed + pid));
    }
  
    while (-encoderLeftCounter < targetTick - 50) {
      pid = computePID();
      md.setSpeeds( -((Set_Speed - pid)- motor_offset1 ),-(Set_Speed + pid));
    }
  
    while (-encoderLeftCounter < targetTick) {
      pid = computePID();
      md.setSpeeds( -((Set_Speed - pid)- motor_offset1 ),-(Set_Speed + pid));
      //Serial.println( " encoderLeftCounter " + String ( encoderLeftCounter) );
      //Serial.println( " targetTick " + String ( targetTick) );
    } 
    // Serial.println( " encoderLeftCounter " + String ( encoderLeftCounter) );
    md.setBrakes(50, 50);
  }
  
  void rotateLeft(double deg) {
    double pid = 0 ;
    float targetTick;
   
    int Set_Speed =  Speed_Spin;
    encoderLeftCounter = 0 ;
    encoderRightCounter = 0 ;
    prevTick = 0 ;
    integral = 0;
    
    if (deg <= 90){
      targetTick = deg * 4.15;
    } 
    else if (deg <= 180) {
      targetTick = deg * 4.34;
    } 
    else if (deg <= 360){
      targetTick = deg * 4.34;
    }
    else{
      targetTick = deg * 4.40;
    }
  
    while ( encoderLeftCounter < min(50, targetTick)) {
      pid = computePID();
      md.setSpeeds( ( (Set_Speed - pid) - motor_offset), -((Set_Speed) + pid));
    }
  
    while (encoderLeftCounter <  targetTick - 50) {
      pid = computePID();
      md.setSpeeds( ( (Set_Speed - pid) - motor_offset), -((Set_Speed) + pid));
    }
    while (encoderLeftCounter < targetTick) {
      pid = computePID();
      md.setSpeeds(( (Set_Speed - pid )- motor_offset), -((Set_Speed) + pid));
    }
    md.setBrakes(Speed_Brake, Speed_Brake);
  }
  
  void rotateRight(double deg) {
  
    double pid = 0;
    float targetTick;
    int Set_Speed = Speed_Spin;
    encoderLeftCounter = 0 ;
    encoderRightCounter = 0 ;
    prevTick = 0 ;
    integral = 0;
  
    if (deg <= 90){
      targetTick = deg * 4.12; //4.08
    } else if (deg <= 180) {
      targetTick = deg * 4.215;
    }
    else if (deg <= 360){
      targetTick = deg * 4.215;
    }
    else{
      targetTick = deg * 4.42;
    }
  
    while ( -encoderLeftCounter < min(50, targetTick)) {
      pid = computePID();
      md.setSpeeds( -( (Set_Speed - pid)- motor_offset), ((Set_Speed) + pid));
      //Serial.println( " encoderLeftCounter " + String ( -encoderLeftCounter) );
      //Serial.println( " targetTick " + String ( targetTick) );
    }
  
    while ( -encoderLeftCounter <  targetTick - 50) {
      pid = computePID();
      md.setSpeeds( -( (Set_Speed - pid)- motor_offset), ((Set_Speed) + pid));
    }
    while ( -encoderLeftCounter < targetTick) {
      pid = computePID();
      md.setSpeeds(-( (Set_Speed - pid )- motor_offset), ((Set_Speed) + pid));
    }
    md.setBrakes(Speed_Brake, Speed_Brake);
  }
  
  /*------------------Calibration ---------------*/
  
  void rotateRightAlign(double deg) {
  
    double pid = 0;
    float targetTick;
    int Set_Speed = Rotate_Spin;
    encoderLeftCounter = 0 ;
    encoderRightCounter = 0 ;
    prevTick = 0 ;
    integral = 0;
  
    if (deg <= 90){
      targetTick = deg * 4.255;
    } else if (deg <= 180) {
      
      targetTick = deg * 4.215;
    }
    else if (deg <= 360){
      targetTick = deg * 4.215;
    }
    else{
      targetTick = deg * 4.42;
    }
  
    while (-encoderLeftCounter < min(50, targetTick)) {
      pid = computePID();
      md.setSpeeds( -( (Set_Speed - pid)- motor_offset), ((Set_Speed) + pid));
      //Serial.println( " encoderLeftCounter " + String ( -encoderLeftCounter) );
      //Serial.println( " targetTick " + String ( targetTick) );
    }
  
    while ( -encoderLeftCounter <  targetTick - 50) {
      pid = computePID();
      md.setSpeeds( -( (Set_Speed - pid)- motor_offset), ((Set_Speed) + pid));
    }
    while ( -encoderLeftCounter < targetTick) {
      pid = computePID();
      md.setSpeeds(-( (Set_Speed - pid )- motor_offset), ((Set_Speed) + pid));
    }
    md.setBrakes(Speed_Brake_Align, Speed_Brake_Align);
  }
  
  void rotateLeftAlign(double deg) {
  
    double pid = 0 ;
    float targetTick;
   
    int Set_Speed =  Rotate_Spin;
    encoderLeftCounter = 0 ;
    encoderRightCounter = 0 ;
    prevTick = 0 ;
    integral = 0;
    
    if (deg <= 90){
      targetTick = deg * 4.21;
    } else if (deg <= 180) {
      targetTick = deg * 4.34;
    }
    else if (deg <= 360){
      targetTick = deg * 4.34;
    }
    else{
      targetTick = deg * 4.40;
    }
  
    while ( encoderLeftCounter < min(50, targetTick)) {
      pid = computePID();
      md.setSpeeds( ( (Set_Speed - pid) - motor_offset), -((Set_Speed) + pid));
    }
  
    while (encoderLeftCounter <  targetTick - 50) {
      pid = computePID();
      md.setSpeeds( ( (Set_Speed - pid) - motor_offset), -((Set_Speed) + pid));
    }
    while (encoderLeftCounter < targetTick) {
      pid = computePID();
      md.setSpeeds(( (Set_Speed - pid )- motor_offset), -((Set_Speed) + pid));
    }
    md.setBrakes(Speed_Brake_Align, Speed_Brake_Align);
  }
  
  void maintainWallFront(){
  int cnt = 0;
  double frontleft;
  double frontback;
  while (cnt<5) {
    cnt++;
    frontleft = final_MedianRead(irFL);
    frontback = final_MedianRead(irFR);
    delay(10);
    if ((frontleft <= 9) && (frontback <= 9)) Reverse(1);
    else if ((frontleft >= 13) && (frontback >= 13)) Forward(1);
    else break;
  }   
}

// 
void maintainWallRight(){
  int cnt = 0;
  double frontleft;
  double frontback;
  
  rotateRight(90);
  delay(50);
  while (cnt<5) { 
    cnt++;
    frontleft = final_MedianRead(irFL);
    frontback = final_MedianRead(irFR);
    delay(15);
    if ((frontleft <= 10) && (frontback <= 10)) Reverse(1);
    else if ((frontleft >= 12) && (frontback >= 12)) Forward(1);
    else break;
  }
  delay(50);
  rotateLeft(90);
}

void maintainWallMiddle(int sensor){
  int cnt = 0;  
  double frontside = final_MedianRead(sensor);
  double frontmid = final_MedianRead(irFM) - 1;
  while ((frontside<= 9) || (frontmid <= 9)){
    Reverse(1);
    frontmid = final_MedianRead(irFM) -1;
    frontside = final_MedianRead(sensor);
  }
}


void maintainWallMiddle2(int sensor){
  int cnt = 0;  
  double frontside = final_MedianRead(sensor);
  double frontmid = final_MedianRead(irFM);
  while (frontside >= 11){
    Forward(1);
    frontside = final_MedianRead(sensor);
  }
}

void calibrate_Robot_Angle() {
    
  int counter = 0;
  
  int leftPos = -1;
  int rightPos = -1;
  double diff;
  double angle;

  double curSensor[3] = {final_MedianRead(irFL), final_MedianRead(irFM), final_MedianRead(irFR)};
  
  // find grid = 1
  for (int i=0; i< 3; i++){
    if (distanceInGrids(curSensor[i]) == 1){
       if (leftPos==-1) leftPos = i;
       else {
          rightPos = i;
       }
    }
  }

  // not in grid
  if (rightPos == -1) return;
  
  // TODO: fix for using middle sensors (still tilted sometimes, due to FM diff from the others)
  if ((rightPos - leftPos)==2) maintainWallFront();
  else if(leftPos == 0) maintainWallMiddle(irFL);
  else maintainWallMiddle(irFR);

  delay(100);
  while(true){
    if (counter >= 15) break;
    counter++;
    
    delay(10);
    
    curSensor[0] = final_MedianRead(irFL);
    curSensor[1] = final_MedianRead(irFM);
    curSensor[2] = final_MedianRead(irFR);
    
    diff = curSensor[rightPos] - curSensor[leftPos];
    angle = atan2(abs(diff), 10);
    angle = angle*3;
    if (diff == 1) angle = angle*2/3;
    
    // TODO: fix calibration, test and debug, not a 100% working code because OF THE GAP
    if((rightPos-leftPos)==1){
      if (rightPos == 2 ){
        angle = angle;
        if (diff >= -1){
          rotateLeftAlign(angle);
        }
        else if (diff <= -2){
          rotateRightAlign(angle); 
        }
        else break;
      }
      else{
        if(diff >= 0.5){
          rotateLeftAlign(angle);
        }
        else if(diff <= -0.5){
          rotateRightAlign(angle);
        }
        else break;
      }
    }
    else if((rightPos-leftPos)==2){
      maintainWallFront();
      
      if(diff >= FRONT_ERR_THRES){
        rotateLeft(angle);
      }
      else if(diff <= -1*FRONT_ERR_THRES){
        rotateRight(angle);
      }
      else break;
    }
  }
  if(rightPos - leftPos == 1) {
    if(leftPos == 0) maintainWallMiddle2(irFL);
    else maintainWallMiddle2(irFR);
  }
  

}
  
void rightAlign(){
  
  calibration_angle = true;
  double distRF;
  double distRB;
  double diffRBF;
  int counter;
  double angle;

  counter = 0;
  
  distRF = final_MedianRead(irRF);
  distRB = final_MedianRead(irRB);

  // ENSURE IN GRID 1, need tweaks
  if ((distanceInGrids(distRF) == 1) && (distanceInGrids(distRB) == 1)) { 
    // if too close
    if ((distRB <= RIGHT_CLOSELIMIT) && (distRF <= RIGHT_CLOSELIMIT)){
      maintainWallRight();
    }
    // if too far
    else if ((distRB >= RIGHT_FARLIMIT) && (distRF >= RIGHT_FARLIMIT)){
      maintainWallRight();
    }
    delay(100);

    if(distRF <= 16 && distRB <=16){
      while (calibration_angle) {
        distRF = final_MedianRead(irRF);
        distRB = final_MedianRead(irRB);
        diffRBF = (distRF - distRB);
  
        // calculate angle, multiply by 2 (center of rotation tolerance)
        angle = 2 * atan2(abs(diffRBF), RIGHT_SENSOR_DIST);
        if (diffRBF == 1) angle = angle * 2 /3;
  
        delay(10);
        if ( counter >= 15) {
          calibration_angle = false;
          break;
        }
    
        if (diffRBF >= RIGHT_ERR_THRES){
          rotateRightAlign(angle);
        } 
        else if (diffRBF <= -1*RIGHT_ERR_THRES){   
          rotateLeftAlign(angle);
        }
        else{
          break;
        }
        counter++;
      }
    }
  }
}
    
  ///////////////////////////////
  void stopIfFault()
  {
    if (md.getM1Fault()){
      //Serial.println("M1 fault");
      while(1);
    }
    if (md.getM2Fault()){
      //Serial.println("M2 fault");
      while(1);
    }
  }
  
  //find median
  //return to algo: SENSOR NAME : Distance in cm : grid Number , repeat for all sensors (FFF, RR, L)
  //OBSTACLE AVOIDANCE - diagonal
  
  void setup()
  {
    
    Serial.begin(115200);
  
    // Sensor Setup 
    pinMode(irFM, INPUT);
    pinMode(irLF, INPUT);
    pinMode(irRF, INPUT);
    pinMode(irFL, INPUT);
    pinMode(irRB, INPUT);
    pinMode(irFR, INPUT);
  
    digitalWrite(irFM, LOW);
    digitalWrite(irLF, LOW);
    digitalWrite(irRF, LOW);
    digitalWrite(irFL, LOW);
    digitalWrite(irRB, LOW);
    digitalWrite(irFR, LOW);
  
    // Motor Setup
    pinMode (encoderpin1a,INPUT);
    pinMode (encoderpin1b,INPUT);
    pinMode (encoderpin2a,INPUT);
    pinMode (encoderpin2b,INPUT);
    
    enableInterrupt(encoderpin1a,encoder1change,RISING); // 3, Enables interrupt on a Arduino pin 3 ( encoderpin1a ) , call the function in rising edge
    enableInterrupt(encoderpin2a,encoder2change,RISING); // 11,  Enables interrupt on a Arduino pin 11 ( encoderpin2a ) call the function  in rising edge
     
    md.init();    // initial motor
  }
  
  void loop2(){
           // if value = 0 , Forward 1 grid, if value != 0. then move with value.
         // delay(1000);
        // rotateRight(90.0);
        //rotateLeft(90.0);
         //rightAlign();
        //calibrate_Robot_Angle();
    //Forward(10);
     //rightAlign();
   //Serial.println(longdistanceInGrids(final_MedianRead(irLF)));
   //Serial.println(final_MedianRead(irLF));
         // calibrate_Robot_Angle();
          Forward(100);
        //  Reverse(1);
       // maintainWallFront();
        //  Forward(1);
          
   //       rightAlign();
   //calibrate_Robot_Angle();
        
         returnSensorReading_Raw();
         delay(1000);
      //    delay(10);
         
         // delay(1000);
          // if value = 0 , Forward 1 grid, if value != 0. then move with value.
        //  returnSensorReading_Raw();
    //      delay(1000);
    
  }
  

  void loop()
  {
  
  delay(2);
    if (!Serial) {
    //  Serial.println("Waiting for connection");
    }
  
  while(1){ 
  
    if( Serial.available()) { 
     
    int value = 0; // Assume to be 0 if it is not indicated
    String data = Serial.readString();
    char command = data.charAt(8);
    
    if (data.length() >= 10) {
      
      value = ((String)(data.charAt(10))).toInt(); //Alg|Ard|0|4
      
    }
      // TODO: debug delays
      switch (command) {
        
        case '0': // Forward
          
          step_counter++;
          (value == 0) ? Forward(10) : Forward(value*10);
          // if value = 0 , Forward 1 grid, if value != 0. then move with value.
          delay(15);
          rightAlign();
          delay(15);
          calibrate_Robot_Angle();
          delay(100);
          returnSensorReading_Raw();
          delay(10);
          
          break;
          
        case '1': // Rotate Left
  
          step_counter++;
          (value == 0) ? rotateLeft(90) : rotateLeft(value*90);
          delay(15);
          rightAlign();
          calibrate_Robot_Angle();
          delay(100);
          returnSensorReading_Raw();
          delay(10);
          
          break;
          
        case '2':  // Rotate Right
        
          step_counter++;
          (value == 0) ? rotateRight(90) : rotateRight(value*90);
          delay(15);
          rightAlign();
          calibrate_Robot_Angle();
          delay(100);
          returnSensorReading_Raw();
          delay(10);
          
          break;
          
        case '3': // Reverse
  
          step_counter++;
          (value == 0) ? Reverse(10) : Reverse(value*10);
          // Serial.println(" Reversing " + value );
          delay(50);
          returnSensorReading_Raw();
          delay(10);
          
          break;
          
        case '4':  // Alight Front
        
         // calibrate_Robot_Position();
         // alignFront();
          calibrate_Robot_Angle();
          delay(10);
          delay(100);
          returnSensorReading_Raw();
          
          break;
          
        case '5':  // Alight Right
  
          
          rightAlign();
          delay(10);
          returnSensorReading_Raw();
          delay(1000);
          break;
  
          
        case '6':  // Start Exploration
  
         //do nothing
          returnSensorReading_Raw();
          break;
         
        case '7': //  Start Fastest Path
        
          fastest_path = true;
         // Serial.println("print something for fastest path");
          returnSensorReading_Raw();
          Serial.flush();
          break;
          
        case '8':  // // Force stop 
        
         md.setBrakes(400, 400);
         returnSensorReading_Raw();       
         break;
          
        case '9': // Set_waypoint ( Do nothing ) 
        
        //
          returnSensorReading_Raw();
          break;
          
        case 'a': // Current position ( Do nothing ) 
        case 'A':
       //
          returnSensorReading_Raw();
          break;
          
        case 's': // Send Sensors data ~~
        case 'S':
  
       // 
          returnSensorReading_Raw();
          break;        
                  
        case 'e': // Error  ( Do nothing )
        case 'E': 
        
        //
          returnSensorReading_Raw();
          break;
  
      }
  
    }
  }
  }
  /*
     //Alg|Ard|0|{1-10} (Steps) [Alg|Ard|0|3]
      //2nd Character of the Array is the Command
  /*
  
  Standards Messages Sent
  
  Format: Sender|Receiver|Action|Data 
  Sender/Receiver:
  Android = And
  Arduino = Ard
  Algorithm = Alg      // Alg|Ard|0|{1-10} (Steps) [Alg|Ard|0|3]
      //2nd Character of the Array is the Command
  *Camera = Cam (KIV)
  Note: Omit white spaces/other forms of delimiters
  
  0  FORWARD
  1 ROT_LEFT
  2 ROT_RIGHT
  3 BACKWARD
  4 START_EXP
  5 START_FP
  6 END_EXP
  7 END_FP
  8 SET_WAYPOINT
  9 CURRENT_POS
  10  ALIGN_FRONT
  11  ALIGN_RIGHT
  12  SEND_SENSORS
  13  STOP
  14  ERROR 
  
  Robot Direction Enum 0-3 (DATA):
  UP, LEFT, DOWN, RIGHT;
  
  
  */