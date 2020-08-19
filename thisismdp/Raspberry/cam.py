import cv2
import numpy as np
from picamera.array import PiRGBArray
from picamera import PiCamera
from time import sleep

# THe Camera object
class Cam(object):
    # Global objects to be used later
    templateU = cv2.imread("up.png", cv2.IMREAD_GRAYSCALE)  # Load the template image
    w, h = templateU.shape[::-1] # Get the original width and height for manipulation later
    templateU = cv2.flip(templateU,1) # Flip it

    def __init__(self):
        self.found = False # Will be true if image is found
        self.sendfound = False # sendfound will be True once found is True. But it will only become False once the m.py sends the image signal
        super(Cam, self).__init__()
    
    def getFound(self):
        return self.found

    def startDetecting(self):
        camera = PiCamera() # Initialise PiCamera
        camera.resolution = (640, 480) # Set the camera resolution
        camera.framerate = 32 # Set the framerate
        rawCapture = PiRGBArray(camera, size=(640, 480)) 
        objectsfound = 0 # Initialise objects found to 0
        for frame in camera.capture_continuous(rawCapture, format="bgr", use_video_port=True): # For each live capture frame
            frame = frame.array # Get the frame array
            gray_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY) #Convert the frame to grayscale for easier processing
            for x in range(80,60, -5): # Loop over a range of X. X will be the range of the scales we want to use for our template to compare to the live capture
                newX, newY = self.w * x * 0.1, self.h * x * 0.1 # Get the width and height we plan to scale the template for comparison later
                ret,thresh = cv2.threshold(self.templateU,160,255,1) # To remove most unimportant details in the template. Any color that is below value 160 will be forced to 255. This eliminates the issues of false detections
                newtemplateU = cv2.resize(thresh, (int(newX), int(newY))) # Resize the template based on the newX and newY
                ret2,gray_frame_thresh = cv2.threshold(gray_frame,160,255,1) # Do the same removal of unimportant details to the live capture. Any color that is below value 160 will be forced to 255. This eliminates the issues of false detections
                resU = cv2.matchTemplate(gray_frame_thresh, newtemplateU, cv2.TM_CCOEFF_NORMED) # use matchTemplate method 
                match = np.where(resU >= 0.8) # Get matches where the similarities is above 0.8
                for pt in zip(*match[::-1]): # For each match
                    objectsfound += 1 # Update the objects found counter
                    cv2.rectangle(gray_frame_thresh, pt, (pt[0] + int(newX), pt[1] + int(newY)), (0, 255, 0), 3) # Draw a rectangle on image (For debugging purpose)
                if (objectsfound > 0): #If there are objects found
                    self.sendfound = True # Set true
                    self.found = True # Set true
                    sleep(4) # sleep to prevent multiple images tobe sent
                    break               
                else:
                    self.found = False # If nothing is found, set false
            
            rawCapture.truncate(0)
            
            # Stop if esc is pressed
            key = cv2.waitKey(1)
            # Reset object counter
            objectsfound = 0
