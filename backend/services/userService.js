import UserModel from "../model/UserModel.js";
import VerificationModel from "../model/VerificationModel.js";
import twilioService from "./twilio/twilioService.js";
import mongoose from "mongoose";

const userService = {
    /**
     * Generate and store a simple verification code
     * @param {string} nombre - User's name
     * @param {string} telefono - User's phone number
     * @param {string} email - User's email (optional)
     * @returns {Promise<Object>} - Service result
     */
    async sendVerificationCode(nombre, telefono, email) {
        try {
            // Clean phone number (remove spaces, dashes, etc.)
            const cleanPhone = '+' + telefono.replace(/\D/g, '');
            
            // Generate a 6-digit verification code
            const verificationCode = Math.floor(100000 + Math.random() * 900000).toString();
            
            // Delete any existing verification for this phone number
            //await VerificationModel.deleteMany({ phoneNumber: cleanPhone });
            
            // Create new verification record
            const verification = new VerificationModel({
                phoneNumber: cleanPhone,
                code: verificationCode,
                name: nombre,
                email: email || '',
                isVerified: false
            });
            console.log("🔍 Estado de conexión Mongoose:", mongoose.connection.readyState);
            
            const response = await verification.save();
            console.log(`✅ Código de verificación generado para ${nombre} (${cleanPhone}): ${verificationCode}`);
            twilioService.sendVerificationCode(cleanPhone, verificationCode)


            return {
                success: true,
                message: 'Código de verificación generado exitosamente',
                data: {
                    phoneNumber: cleanPhone,
                    code: verificationCode, // En producción, no deberías devolver el código
                    expiresIn: '5 minutos'
                }
            };
        } catch (error) {
            console.error('Error in sendVerificationCode:', error.message);
            throw new Error(`Error generating verification code: ${error.message}`);
        }
    },

    /**
     * Verify the code entered by the user
     * @param {string} telefono - User's phone number
     * @param {string} code - Verification code
     * @returns {Promise<Object>} - Verification result
     */
    async verifyCode(telefono, code) {
        try {
            const cleanPhone = '+' + telefono.replace(/\D/g, '');
            console.log(`Verifying code for phone: ${cleanPhone} with code: ${code}`);
            // Find the verification record
            const verification = await VerificationModel.findOne({
                phoneNumber: cleanPhone,
                code: code,
                isVerified: false
            });

            console.log(verification)
            
            if (!verification) {
                return {
                    success: false,
                    message: 'Código de verificación inválido o expirado',
                    verified: false
                };
            }
            
            // Mark as verified
            verification.isVerified = true;
            await verification.save();
            
            console.log(`✅ Código verificado exitosamente para ${verification.name} (${cleanPhone})`);
            
            return {
                success: true,
                message: 'Código verificado exitosamente',
                verified: true,
                userData: {
                    name: verification.name,
                    phoneNumber: verification.phoneNumber,
                    email: verification.email
                }
            };
        } catch (error) {
            console.error('Error in verifyCode:', error.message);
            throw new Error(`Error verifying code: ${error.message}`);
        }
    },

    /**
     * Get verification status
     * @param {string} telefono - User's phone number
     * @returns {Promise<Object>} - Verification status
     */
    async getVerificationStatus(telefono) {
        try {
            const cleanPhone = telefono.replace(/\D/g, '');
            
            const verification = await VerificationModel.findOne({
                phoneNumber: cleanPhone
            }).sort({ createdAt: -1 });
            
            if (!verification) {
                return {
                    success: false,
                    message: 'No se encontró solicitud de verificación',
                    status: 'not_found'
                };
            }
            
            return {
                success: true,
                status: verification.isVerified ? 'verified' : 'pending',
                message: verification.isVerified ? 'Número verificado' : 'Verificación pendiente',
                data: {
                    name: verification.name,
                    phoneNumber: verification.phoneNumber,
                    createdAt: verification.createdAt,
                    isVerified: verification.isVerified
                }
            };
        } catch (error) {
            console.error('Error in getVerificationStatus:', error.message);
            throw new Error(`Error checking verification status: ${error.message}`);
        }
    },

    /**
     * Send emergency alert (simple version without Twilio)
     * @param {string} userId - User ID
     * @param {string} alertMessage - Emergency message
     * @returns {Promise<Object>} - Alert result
     */
    async sendEmergencyAlert(userId, alertMessage) {
        try {
            // Get user and their emergency contacts
            const user = await UserModel.findById(userId);
            if (!user) {
                throw new Error('User not found');
            }

            if (!user.emergencyContacts || user.emergencyContacts.length === 0) {
                throw new Error('No emergency contacts found for this user');
            }

            // Log the emergency alert (in a real app, you would integrate with a real SMS service)
            console.log('🚨 EMERGENCY ALERT 🚨');
            console.log(`User: ${user.name} (${user.phoneNumber})`);
            console.log(`Message: ${alertMessage}`);
            console.log('Emergency contacts to notify:');
            
            user.emergencyContacts.forEach((contact, index) => {
                console.log(`${index + 1}. ${contact.name} (${contact.relationship}): ${contact.phone}`);
            });

            return {
                success: true,
                message: `Emergency alert logged for ${user.emergencyContacts.length} contacts`,
                totalContacts: user.emergencyContacts.length,
                contacts: user.emergencyContacts.map(contact => ({
                    name: contact.name,
                    phone: contact.phone,
                    relationship: contact.relationship
                }))
            };
        } catch (error) {
            console.error('Error in sendEmergencyAlert:', error.message);
            throw new Error(`Error sending emergency alert: ${error.message}`);
        }
    },

    async createUserProfile(nombre, telefono, email, contactoEmergencia) {
        try {
            const cleanPhone = telefono.replace(/\D/g, '');
            const newUser = new UserModel({
                name: nombre,
                phoneNumber: cleanPhone,
                email: email || '',
                emergencyContacts: contactoEmergencia || []
            });
            const savedUser = await newUser.save();
            return {
                success: true,
                message: 'User profile created successfully',
                data: {
                    userId: savedUser._id,
                    name: savedUser.name,
                    phoneNumber: savedUser.phoneNumber,
                    email: savedUser.email
                }
            };
        } catch (error) {
            console.error('Error in createUserProfile:', error.message);
            throw new Error(`Error creating user profile: ${error.message}`);
        }
    }
    
};

export default userService;