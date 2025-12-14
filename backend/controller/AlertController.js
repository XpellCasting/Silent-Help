import AlertModel from "../model/AlertModel.js";

const createAlert = async (req, res) => {
    try {
        const { userId, direccion, audio_base64, startTime, endTime, duration, date } = req.body;

        const newAlert = new AlertModel({
            userId,
            direccion,
            audio_base64,
            startTime,
            endTime,
            date,
            duration
        });

        await newAlert.save();

        res.status(201).json({
            message: "Alerta creada exitosamente",
            alert: newAlert
        });
    } catch (error) {
        console.error("Error al crear alerta:", error);
        res.status(500).json({
            message: "Error interno del servidor al guardar la alerta",
            error: error.message
        });
    }
};

const getAlertsByUser = async (req, res) => {
    try {
        const { userId } = req.params;
        const alerts = await AlertModel.find({ userId }).sort({ createdAt: -1 });
        res.status(200).json(alerts);
    } catch (error) {
        console.error("Error al obtener historial:", error);
        res.status(500).json({ message: "Error al obtener historial" });
    }
};


export default {
    createAlert,
    getAlertsByUser
};
