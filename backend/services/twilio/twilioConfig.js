import dotenv from 'dotenv';

dotenv.config();

const twilioConfig = {
    accountSid: process.env.TWILIO_ACCOUNT_SID,
    authToken: process.env.TWILIO_AUTH_TOKEN,
    phoneNumber: process.env.TWILIO_PHONE_NUMBER,
    serviceSid: process.env.TWILIO_VERIFY_SERVICE_SID, // For Verify API
};

// Validate required environment variables
const validateConfig = () => {
    const requiredVars = ['TWILIO_ACCOUNT_SID', 'TWILIO_AUTH_TOKEN'];
    const missingVars = requiredVars.filter(varName => !process.env[varName]);
    
    if (missingVars.length > 0) {
        throw new Error(`Missing required Twilio environment variables: ${missingVars.join(', ')}`);
    }
};

export { twilioConfig, validateConfig };