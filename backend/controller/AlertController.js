import AlertModel from "../model/AlertModel.js";

const createAlert = async (req, res) => {
    try {
        const { userId, direccion, audios, startTime, endTime, duration, date } = req.body;

        const newAlert = new AlertModel({
            userId,
            direccion,
            audios: audios || [],
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

const addAudioToAlert = async (req, res) => {
    try {
        const { alertId } = req.params;
        const { audio_base64 } = req.body;

        if (!audio_base64) {
            return res.status(400).json({ message: "Falta audio_base64" });
        }

        const alert = await AlertModel.findById(alertId);
        if (!alert) {
            return res.status(404).json({ message: "Alerta no encontrada" });
        }

        // Inicializar array si no existe (para alertas viejas)
        if (!alert.audios) {
            alert.audios = [];
        }

        alert.audios.push(audio_base64);
        await alert.save();

        res.status(200).json({ message: "Audio agregado correctamente", alertId: alert._id });
    } catch (error) {
        console.error("Error al agregar audio:", error);
        res.status(500).json({ message: "Error al agregar audio", error });
    }
};

const endAlert = async (req, res) => {
    try {
        const { alertId } = req.params;
        const { endTime, duration } = req.body;

        const alert = await AlertModel.findById(alertId);
        if (!alert) {
            return res.status(404).json({ message: "Alerta no encontrada" });
        }

        alert.endTime = endTime;
        alert.duration = duration;
        alert.status = "Finalizada"; // Opcional, si agregamos el campo status al modelo

        await alert.save();

        res.status(200).json({ message: "Alerta finalizada correctamente", alert });
    } catch (error) {
        console.error("Error al finalizar alerta:", error);
        res.status(500).json({ message: "Error al finalizar alerta", error });
    }
};

export default {
    createAlert,
    getAlertsByUser,
    addAudioToAlert,
    endAlert
};
