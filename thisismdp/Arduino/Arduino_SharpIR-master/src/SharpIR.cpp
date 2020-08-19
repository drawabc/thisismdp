#include "SharpIR.h"

double SharpIR::getDistance( bool avoidBurstRead )
	{
		double distance ;

		if( !avoidBurstRead ) while( millis() <= lastTime + 20 ) {} //wait for sensor's sampling time

		lastTime = millis();

		switch( sensorType )
		{
			case GP2Y0A41SK0F :

				distance = (double) 2076/(analogRead(pin)-11);

				if(distance > 30) return 31;
				else if(distance < 4) return 3;
				else return distance;

				break;

			case GP2Y0A21YK0F :
				if (analogRead(pin) <= 80) return 81;

				distance = (double) 4800/(analogRead(pin)-20);
				if(distance > 80) return 81;
				else if(distance < 9) return 9;
				else return distance;

				break;

			case GP2Y0A02YK0F :
				if (analogRead(pin) <= 80) return 151;
				distance = (double) 9462/(analogRead(pin)-16.92);

				if(distance > 150) return 151;
				else if(distance < 20) return 19;
				else return distance;
		}
	}
