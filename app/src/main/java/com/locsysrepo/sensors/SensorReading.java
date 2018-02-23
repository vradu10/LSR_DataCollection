package com.locsysrepo.sensors;

public class SensorReading {

    private InertialSensorManager.SensorEnum type;
    private float x, y, z;
    private long timestamp;
    private long sensor_timestamp;
    private StringBuffer sb = new StringBuffer();


    public SensorReading(float x, float y, float z, long time, InertialSensorManager.SensorEnum type, long sensor_timestamp) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = time;
        this.type = type;
        this.sensor_timestamp = sensor_timestamp;
    }

    public String toString() {
        sb.setLength(0);

        sb.append("<");
        sb.append(type.getTag());
        sb.append(" t=\""); sb.append(timestamp);
        sb.append("\" x=\""); sb.append(x);
        sb.append("\" y=\""); sb.append(y);
        sb.append("\" z=\""); sb.append(z);
        sb.append("\" st=\""); sb.append(sensor_timestamp);
        sb.append("\" />");

        return sb.toString();
    }

    public InertialSensorManager.SensorEnum getType() {
        return type;
    }
}