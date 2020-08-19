import cv2 as cv
import picamera
import picamera.array
import time
import imutils
import numpy as np

class Cam(object):

    template = 'arrow_u.png'
    #0.43
    #0.435
    scales = [0.44]
    storedTemplates = {}
    gray_template = cv.cvtColor(cv.imread(template), cv.COLOR_BGR2GRAY)

    def __init__(self):
            self.storingTemplateScale(self.gray_template)
            print ("Templates stored")
            self.arrow = False


    def storingTemplateScale(self, template):
            for scale in self.scales:
                    resized = imutils.resize(template, width=int(template.shape[1]*scale))
                    self.storedTemplates[str(scale)] = resized


    def multiScaleTemplateMatching(self, image):

            image_gray = cv.cvtColor(image, cv.COLOR_RGB2GRAY)
            #time.sleep(1)
            for scale in self.scales:

                    self.arrow = False
                    w = h = 0
                    template = self.storedTemplates[str(scale)]
                    w, h = template.shape[::-1]
                    res = cv.matchTemplate(image_gray, template, cv.TM_CCOEFF_NORMED)
                    minVal, maxVal, minLoc, maxLoc = cv.minMaxLoc(res)

                    if maxVal < 0.8:
                            continue

                    else:
                            #top_left = maxLoc
                            #bottom_right = (top_left[0] + w, top_left[1] + h)
                            self.arrow = True
                            time.sleep(1)
                            #print ("spot")

                    #cv.rectangle(image, top_left, bottom_right, 255, 2)




    def ready(self):

        print ("Camera Initialized")
        with picamera.PiCamera() as self.camera:
            with picamera.array.PiRGBArray(self.camera) as self.stream:
                self.camera.resolution = (320, 240)
                time.sleep(0.1)

		#self.storingTemplateScale(gray_template)

                while 1:
                        self.camera.capture(self.stream, format='bgr', use_video_port=True)
                        frame = self.stream.array
                        #cropped = frame[90:320, 0:500]
                        cropped = frame[70:320, 0:500]
                        self.multiScaleTemplateMatching(cropped)
                        #cv.imshow('OG', cropped)
                        #if cv.waitKey(1) & 0xFF == ord('q'):
                        #        break
                        self.stream.seek(0)
                        self.stream.truncate()

    def arrow_seen(self):
        return self.arrow

if __name__ == "__main__":

	try:
            c = Cam()
            c.ready()
            print ("Camera Connected")

	except KeyboardInterrupt:
		sr.disconnect()





