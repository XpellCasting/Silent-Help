import { twilioConfig } from './twilioConfig.js';

class TwilioService { // Mantenemos el nombre para no romper imports
    constructor() {
        // No necesitamos inicializar cliente de Twilio
        console.log("✅ TextBee Service Initialized");
    }

    /**
     * Send SMS verification code using TextBee
     */
    async sendVerificationCode(phoneNumber, code = null) {
        try {
            const message = `Tu código de verificación es: ${code}. Este código expira en 5 minutos.`;
            return await this.sendSMS(phoneNumber, message);
        } catch (error) {
            console.error('❌ Error sending verification code:', error.message);
            throw new Error(`Failed to send verification code: ${error.message}`);
        }
    }

    /**
     * Send a simple SMS message via TextBee
     */
    async sendSMS(to, message) {
        try {
            const apiKey = twilioConfig.textbeeApiKey;
            const deviceId = twilioConfig.textbeeDeviceId;

            if (!apiKey || !deviceId) {
                console.error("❌ Faltan credenciales de TextBee (TEXTBEE_API_KEY o TEXTBEE_DEVICE_ID)");
                throw new Error("Missing TextBee credentials");
            }

            // TextBee espera recipients como array
            let recipients = Array.isArray(to) ? to : [to];
            
            // Limpiar números
            recipients = recipients.map(num => this.formatPhoneNumber(num));

            console.log("📤 Sending SMS to:", recipients);

            const url = `https://api.textbee.dev/api/v1/gateway/devices/${deviceId}/sendSMS`;
            
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'x-api-key': apiKey,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    recipients: recipients,
                    message: message,
                    priority: "high" // Opcional, depende de TextBee
                })
            });

            const data = await response.json();
            
            console.log("📨 TextBee Response:", data);

            if (!response.ok) {
                console.error("❌ TextBee Error Response:", data);
                throw new Error(data.error || 'Error en TextBee API');
            }

            return {
                success: true,
                sid: data._id || 'textbee_id', // TextBee devuelve un ID
                status: 'queued',
                message: 'SMS sent via TextBee'
            };

        } catch (error) {
            console.error('❌ Error sending SMS via TextBee:', error.message);
            // No lanzamos error para no tumbar el servidor, pero retornamos success: false
            return {
                success: false,
                error: error.message
            };
        }
    }

    /**
     * Send emergency alert SMS to multiple contacts
     */
    async sendEmergencyAlert(phoneNumbers, alertMessage, userInfo = {}) {
        try {
            const fullMessage = `🚨 ALERTA DE EMERGENCIA 🚨\n\n${alertMessage}\n\nUsuario: ${userInfo.name || 'Usuario'}\nTeléfono: ${userInfo.phone || 'No disponible'}\n\nUbicación: ${userInfo.location || ''}`;
            
            // TextBee soporta múltiples destinatarios en una sola llamada
            return await this.sendSMS(phoneNumbers, fullMessage);
            
        } catch (error) {
            console.error('❌ Error sending emergency alerts:', error.message);
            throw new Error(`Failed to send emergency alerts: ${error.message}`);
        }
    }

    formatPhoneNumber(phoneNumber, countryCode = '+56') {
        let cleaned = phoneNumber.replace(/\D/g, '');
        if (cleaned.length > 10 && cleaned.startsWith('56')) return '+' + cleaned;
        if (cleaned.length === 10) return countryCode + cleaned;
        if (phoneNumber.startsWith('+')) return phoneNumber;
        return countryCode + cleaned;
    }
}

const twilioService = new TwilioService();
export default twilioService;