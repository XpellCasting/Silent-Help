import AlertModel from "../model/AlertModel.js";
import UserModel from "../model/UserModel.js";
import twilioService from "../services/twilio/twilioService.js";

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

        // 🔹 Notificar a contactos de emergencia
        let notifiedContacts = [];
        try {
            const user = await UserModel.findById(userId);
            console.log("👤 Usuario encontrado:", user ? user.name : "No encontrado");
            if (user && user.emergencyContacts) {
                console.log(`📋 Contactos encontrados: ${user.emergencyContacts.length}`);
                user.emergencyContacts.forEach(c => console.log(`   - ${c.name}: ${c.phone}`));
            }

            if (user && user.emergencyContacts && user.emergencyContacts.length > 0) {
                // VERIFICACIÓN: Solo enviar SMS si la dirección es REAL.
                // Si es "Ubicación en proceso...", esperamos a updateLocation.
                const isProvisional = direccion && (direccion.includes("proceso") || direccion.includes("Buscando"));

                if (!isProvisional) {
                    const contactPhones = user.emergencyContacts.map(contact => contact.phone);

                    // Mensaje personalizado solicitado
                    const customMessage = "puede que me robaron manin";

                    console.log(`Enviando alerta a ${contactPhones.length} contactos...`);

                    const smsResult = await twilioService.sendEmergencyAlert(contactPhones, customMessage, {
                        name: user.name,
                        phone: user.phoneNumber,
                        location: direccion
                    });

                    if (smsResult.success) {
                        notifiedContacts = user.emergencyContacts.map(c => c.name);
                    }
                } else {
                    console.log("⏳ Dirección provisional detectada. SMS pospuesto hasta tener ubicación real.");
                }
            }
        } catch (smsError) {
            console.error("Error enviando SMS de alerta:", smsError);
        }

        res.status(201).json({
            message: "Alerta creada exitosamente",
            alert: newAlert,
            notifiedContacts: notifiedContacts
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

const updateLocation = async (req, res) => {
    try {
        const { alertId } = req.params;
        const { latitude, longitude, direccion } = req.body;

        if (latitude === undefined || longitude === undefined) {
            return res.status(400).json({ message: "Faltan coordenadas (latitude, longitude)" });
        }

        const alert = await AlertModel.findById(alertId);
        if (!alert) {
            return res.status(404).json({ message: "Alerta no encontrada" });
        }

        // Verificar si debemos notificar actualización de ubicación (Solo si pasa de "Proceso" a Real)
        let shouldNotifyUpdate = false;
        if (direccion && alert.direccion &&
            (alert.direccion.includes("proceso") || alert.direccion.includes("Buscando"))) {
            if (!direccion.includes("proceso") && !direccion.includes("Buscando")) {
                shouldNotifyUpdate = true;
            }
        }

        // Actualizar dirección si se proporciona
        if (direccion) {
            alert.direccion = direccion;
        }

        // Agregar nueva ubicación al historial
        alert.locationHistory.push({
            latitude,
            longitude,
            timestamp: new Date()
        });

        await alert.save();

        // Enviar SMS de actualización si corresponde
        if (shouldNotifyUpdate) {
            try {
                const user = await UserModel.findById(alert.userId);
                if (user && user.emergencyContacts && user.emergencyContacts.length > 0) {
                    const contactPhones = user.emergencyContacts.map(c => c.phone);
                    console.log(`📍 Ubicación corregida. Enviando ALERTA REAL a ${contactPhones.length} contactos.`);

                    // Usamos el mensaje original porque este ES el primer mensaje real que reciben
                    const alertMessage = "puede que me robaron manin";

                    await twilioService.sendEmergencyAlert(contactPhones, alertMessage, {
                        name: user.name,
                        phone: user.phoneNumber,
                        location: direccion
                    });
                }
            } catch (notifyError) {
                console.error("Error enviando SMS de actualización:", notifyError);
            }
        }

        res.status(200).json({ message: "Ubicación actualizada correctamente" });
    } catch (error) {
        console.error("Error al actualizar ubicación:", error);
        res.status(500).json({ message: "Error al actualizar ubicación", error });
    }
};

export default {
    createAlert,
    getAlertsByUser,
    addAudioToAlert,
    endAlert,
    updateLocation
};
