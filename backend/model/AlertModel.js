import mongoose from "mongoose";

const AlertSchema = new mongoose.Schema({
    userId: {
        type: String,
        required: true
    },
    direccion: {
        type: String,
        required: true
    },
    locationHistory: [{
        latitude: { type: Number },
        longitude: { type: Number },
        timestamp: { type: Date, default: Date.now }
    }],
    audios: [{ type: String }],     // Lista de audios adicionales
    startTime: {
        type: String, // "HH:mm:ss"
        required: true
    },
    endTime: {
        type: String, // "HH:mm:ss"
        required: true
    },
    date: {
        type: String, // "dd/MM/yyyy"
        required: true
    },
    duration: {
        type: String, // Ejemplo: "00:01:23"
        required: true
    },
    status: {
        type: String,
        default: "En curso"
    },
    createdAt: {
        type: Date,
        default: Date.now
    }
});

export default mongoose.model("Alert", AlertSchema);
