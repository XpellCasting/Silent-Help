import dotenv from 'dotenv';

dotenv.config();

const twilioConfig = {
    accountSid: process.env.TWILIO_ACCOUNT_SID,
    authToken: process.env.TWILIO_AUTH_TOKEN,
    phoneNumber: process.env.TWILIO_PHONE_NUMBER,
    serviceSid: process.env.TWILIO_VERIFY_SERVICE_SID, // For Verify API
    // TextBee Configuration
    textbeeApiKey: process.env.TEXTBEE_API_KEY,
    textbeeDeviceId: process.env.TEXTBEE_DEVICE_ID
};

// Validate required environment variables
const validateConfig = () => {
    // Para TextBee no validamos Twilio obligatoriamente si vamos a usar TextBee
    // Pero mantenemos la estructura por si acaso.
    // const requiredVars = ['TEXTBEE_API_KEY', 'TEXTBEE_DEVICE_ID']; 
};

export { twilioConfig, validateConfig };