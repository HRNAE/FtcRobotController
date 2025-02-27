package org.firstinspires.ftc.teamcode;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
//import org.firstinspires.ftc.teamcode.Hardware.Hware;
import org.firstinspires.ftc.teamcode.drive.SampleMecanumDrive;
import org.firstinspires.ftc.teamcode.trajectorysequence.TrajectorySequence;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;
import org.openftc.easyopencv.OpenCvPipeline;

import java.util.ArrayList;
import java.util.List;

@TeleOp(name = "redRightOpenCV25")

public class redRightOpenCV25pts extends LinearOpMode {
    Hware robot;
    double cX = 0;
    double cY = 0;
    double width = 0;
    double distance;

    private OpenCvCamera controlHubCam;  // Use OpenCvCamera class from FTC SDK
    private static final int CAMERA_WIDTH = 1280; // width  of wanted camera resolution(Old: 640)
    private static final int CAMERA_HEIGHT = 960; // height of wanted camera resolution(Old : 360)

    // Calculate the distance using the formula
    public static final double objectWidthInRealWorldUnits = 3.75;  // Replace with the actual width of the object in real-world units
    public static final double focalLength = 1606.8;  // Replace with the focal length of the camera in pixels


    @Override
    public void runOpMode() {
        robot = new Hware(hardwareMap);

        initOpenCV();
        FtcDashboard dashboard = FtcDashboard.getInstance();
        telemetry = new MultipleTelemetry(telemetry, dashboard.getTelemetry());
        FtcDashboard.getInstance().startCameraStream(controlHubCam, 30);

        telemetry.addData("Coordinate", "(" + (int) cX + ", " + (int) cY + ")");
        telemetry.addData("Distance in Inch", (getDistance(width)));
        telemetry.update();


        waitForStart();
        distance = getDistance(width) - 10;

        SampleMecanumDrive drive = new SampleMecanumDrive(hardwareMap);

        Pose2d startPose2 = new Pose2d(12, -36, 270);
        Pose2d startPose1 = new Pose2d(12, -60, 270);

        //drive.setPoseEstimate(startPose);
        TrajectorySequence forward = drive.trajectorySequenceBuilder(startPose1)
                .lineToSplineHeading(new Pose2d(12,-34,Math.toRadians(180)))
                .build();
        TrajectorySequence left = drive.trajectorySequenceBuilder(startPose2)
                .lineToSplineHeading(new Pose2d(12,-34,Math.toRadians(180)))

                .UNSTABLE_addTemporalMarkerOffset(1, () -> { //outtake
                    robot.intakeRight.setPower(1);
                    robot.intakeLeft.setPower(1);
                })
                .UNSTABLE_addTemporalMarkerOffset(1.5, () -> { //outtake
                    robot.intakeRight.setPower(0);
                    robot.intakeLeft.setPower(0);
                })


                .waitSeconds(2)
                .build();

        TrajectorySequence right = drive.trajectorySequenceBuilder(startPose2)
                .lineToSplineHeading(new Pose2d(12,-34,Math.toRadians(180)))

                .UNSTABLE_addTemporalMarkerOffset(1, () -> { //outtake
                    robot.intakeRight.setPower(1);
                    robot.intakeLeft.setPower(1);
                })
                .UNSTABLE_addTemporalMarkerOffset(1.5, () -> { //outtake
                    robot.intakeRight.setPower(0);
                    robot.intakeLeft.setPower(0);
                })


                .waitSeconds(2)
                .build();
        TrajectorySequence center = drive.trajectorySequenceBuilder(startPose2)
                .lineToSplineHeading(new Pose2d(12,-34,Math.toRadians(180)))
                .lineToSplineHeading(new Pose2d(25,-25,Math.toRadians(180)))

                .UNSTABLE_addTemporalMarkerOffset(1, () -> { //outtake
                    robot.intakeRight.setPower(1);
                    robot.intakeLeft.setPower(1);
                })
                .UNSTABLE_addTemporalMarkerOffset(1.5, () -> { //outtake
                    robot.intakeRight.setPower(0);
                    robot.intakeLeft.setPower(0);
                })


                .waitSeconds(2)
                .build();

        while (opModeIsActive()) {
            telemetry.addData("Coordinate", "(" + (int) cX + ", " + (int) cY + ")");
            telemetry.addData("Distance in Inch", (getDistance(width)));
            telemetry.update();


            if(cX < 320) {
                telemetry.addData("Direction: ", "left");
                drive.followTrajectorySequence(forward);
                drive.followTrajectorySequence(left);

            }
            else if ((cX > 320) && (cX < 960)) {
                telemetry.addData("Direction: ", "center");
                drive.followTrajectorySequence(forward);
                drive.followTrajectorySequence(center);
            }
            else {
                telemetry.addData("Direction: ", "right");
                drive.followTrajectorySequence(forward);
                drive.followTrajectorySequence(right);
            }

            sleep(30000);// The OpenCV pipeline automatically processes frames and handles detection
        }

        // Release resources
        controlHubCam.stopStreaming();
    }

    private void initOpenCV() {

        // Create an instance of the camera
        int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier(
                "cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());

        // Use OpenCvCameraFactory class from FTC SDK to create camera instance
        controlHubCam = OpenCvCameraFactory.getInstance().createWebcam(
                hardwareMap.get(WebcamName.class, "Webcam 1"), cameraMonitorViewId);

        controlHubCam.setPipeline(new YellowBlobDetectionPipeline());

        controlHubCam.openCameraDevice();
        controlHubCam.startStreaming(CAMERA_WIDTH, CAMERA_HEIGHT, OpenCvCameraRotation.UPRIGHT);
    }
    class YellowBlobDetectionPipeline extends OpenCvPipeline {
        @Override
        public Mat processFrame(Mat input) {
            // Preprocess the frame to detect yellow regions
            Mat yellowMask = preprocessFrame(input);

            // Find contours of the detected yellow regions
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(yellowMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            // Find the largest yellow contour (blob)
            MatOfPoint largestContour = findLargestContour(contours);

            if (largestContour != null) {
                // Draw a red outline around the largest detected object
                Imgproc.drawContours(input, contours, contours.indexOf(largestContour), new Scalar(255, 0, 0), 2);
                // Calculate the width of the bounding box
                width = calculateWidth(largestContour);

                // Display the width next to the label
                String widthLabel = "Width: " + (int) width + " pixels";
                Imgproc.putText(input, widthLabel, new Point(cX + 10, cY + 20), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 255, 0), 2);
                //Display the Distance
                String distanceLabel = "Distance: " + String.format("%.2f", getDistance(width)) + " inches";
                Imgproc.putText(input, distanceLabel, new Point(cX + 10, cY + 60), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 255, 0), 2);
                // Calculate the centroid of the largest contour
                Moments moments = Imgproc.moments(largestContour);
                cX = moments.get_m10() / moments.get_m00();
                cY = moments.get_m01() / moments.get_m00();

                // Draw a dot at the centroid
                String label = "(" + (int) cX + ", " + (int) cY + ")";
                Imgproc.putText(input, label, new Point(cX + 10, cY), Imgproc.FONT_HERSHEY_COMPLEX, 0.5, new Scalar(0, 255, 0), 2);
                Imgproc.circle(input, new Point(cX, cY), 5, new Scalar(0, 255, 0), -1);

            }

            return input;
        }

        private Mat preprocessFrame(Mat frame) {
            Mat hsvFrame = new Mat();
            Imgproc.cvtColor(frame, hsvFrame, Imgproc.COLOR_BGR2HSV);

            Scalar lowerYellow = new Scalar(100, 100, 100);
            Scalar upperYellow = new Scalar(200, 255, 255);


            Mat yellowMask = new Mat();
            Core.inRange(hsvFrame, lowerYellow, upperYellow, yellowMask);

            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
            Imgproc.morphologyEx(yellowMask, yellowMask, Imgproc.MORPH_OPEN, kernel);
            Imgproc.morphologyEx(yellowMask, yellowMask, Imgproc.MORPH_CLOSE, kernel);

            return yellowMask;
        }

        private MatOfPoint findLargestContour(List<MatOfPoint> contours) {
            double maxArea = 0;
            MatOfPoint largestContour = null;

            for (MatOfPoint contour : contours) {
                double area = Imgproc.contourArea(contour);
                if (area > maxArea) {
                    maxArea = area;
                    largestContour = contour;
                }
            }

            return largestContour;
        }
        private double calculateWidth(MatOfPoint contour) {
            Rect boundingRect = Imgproc.boundingRect(contour);
            return boundingRect.width;
        }

    }
    private static double getDistance(double width){
        double distance = (objectWidthInRealWorldUnits * focalLength) / width;
        return distance;
    }

}

