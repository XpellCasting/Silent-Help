import twilio from 'twilio';
import { twilioConfig, validateConfig } from './twilioConfig.js';

class TwilioService {
    constructor() {
        try {
            validateConfig();
            this.client = twilio(twilioConfig.accountSid, twilioConfig.authToken);
        } catch (error) {
            console.error('❌ Twilio configuration error:', error.message);
            throw error;
        }
    }

    /**
     * Send SMS verification code using direct SMS
     * @param {string} phoneNumber - Phone number in E.164 format (e.g., +1234567890)
     * @returns {Promise<Object>} - Verification result with code
     */
    async sendVerificationCode(phoneNumber, code = null) {
        try {
            if (!twilioConfig.phoneNumber) {
                throw new Error('TWILIO_PHONE_NUMBER is required for sending verification codes');
            }
            
            // Create the verification message
            const message = `Tu código de verificación es: ${code}. Este código expira en 5 minutos.`;

            // Send SMS with the verification code
            const sms = await this.client.messages.create({
                body: message,
                from: twilioConfig.phoneNumber,
                to: phoneNumber
            });

            return {
                success: true,
                sid: sms.sid,
                status: sms.status,
                code: code, // Return the code so it can be stored/compared
                message: 'Verification code sent successfully'
            };
        } catch (error) {
            console.error('❌ Error sending verification code:', error.message);
            throw new Error(`Failed to send verification code: ${error.message}`);
        }
    }

    // Note: Code verification is handled in the application logic with database lookup
    // This service only handles sending SMS codes

    /**
     * Send a simple SMS message
     * @param {string} to - Recipient phone number in E.164 format
     * @param {string} message - Message content
     * @returns {Promise<Object>} - SMS result
     */
    async sendSMS(to, message) {
        try {
            if (!twilioConfig.phoneNumber) {
                throw new Error('TWILIO_PHONE_NUMBER is required for sending SMS');
            }

            const sms = await this.client.messages.create({
                body: message,
                from: twilioConfig.phoneNumber,
                to: to
            });

            return {
                success: true,
                sid: sms.sid,
                status: sms.status,
                message: 'SMS sent successfully'
            };
        } catch (error) {
            console.error('❌ Error sending SMS:', error.message);
            throw new Error(`Failed to send SMS: ${error.message}`);
        }
    }

    /**
     * Send emergency alert SMS to multiple contacts
     * @param {Array<string>} phoneNumbers - Array of phone numbers
     * @param {string} alertMessage - Emergency message
     * @param {Object} userInfo - User information for context
     * @returns {Promise<Object>} - Bulk SMS results
     */
    async sendEmergencyAlert(phoneNumbers, alertMessage, userInfo = {}) {
        try {
            const results = [];
            const fullMessage = `🚨 ALERTA DE EMERGENCIA 🚨\n\n${alertMessage}\n\nUsuario: ${userInfo.name || 'Usuario'}\nTeléfono: ${userInfo.phone || 'No disponible'}\n\nEste es un mensaje automático del sistema de seguridad.`;

            for (const phoneNumber of phoneNumbers) {
                try {
                    const result = await this.sendSMS(phoneNumber, fullMessage);
                    results.push({
                        phoneNumber,
                        success: true,
                        sid: result.sid
                    });
                } catch (error) {
                    results.push({
                        phoneNumber,
                        success: false,
                        error: error.message
                    });
                }
            }

            const successCount = results.filter(r => r.success).length;
            
            return {
                success: successCount > 0,
                totalSent: successCount,
                totalFailed: results.length - successCount,
                results: results
            };
        } catch (error) {
            console.error('❌ Error sending emergency alerts:', error.message);
            throw new Error(`Failed to send emergency alerts: ${error.message}`);
        }
    }

    /**
     * Format phone number to E.164 format
     * @param {string} phoneNumber - Phone number in any format
     * @param {string} countryCode - Default country code (e.g., '+52' for Mexico)
     * @returns {string} - Formatted phone number
     */
    formatPhoneNumber(phoneNumber, countryCode = '+52') {
        // Remove all non-digit characters
        let cleaned = phoneNumber.replace(/\D/g, '');
        
        // If it starts with country code digits, add the +
        if (cleaned.length > 10 && cleaned.startsWith('52')) {
            return '+' + cleaned;
        }
        
        // If it's a local number, add country code
        if (cleaned.length === 10) {
            return countryCode + cleaned;
        }
        
        // If it already has + return as is
        if (phoneNumber.startsWith('+')) {
            return phoneNumber;
        }
        
        return countryCode + cleaned;
    }
}

// Export singleton instance
const twilioService = new TwilioService();
export default twilioService;