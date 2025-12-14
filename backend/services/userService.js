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
                email: email || undefined,
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
                email: email || undefined,
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
    },

    /**
     * Get user profile by phone number
     * @param {string} telefono - User's phone number
     * @returns {Promise<Object>} - User profile data
     */
    async getUserByPhone(telefono) {
        try {
            const cleanPhone = telefono.replace(/\D/g, '');
            
            const user = await UserModel.findOne({ phoneNumber: cleanPhone });
            
            if (!user) {
                return {
                    success: false,
                    message: 'Usuario no encontrado',
                    data: null
                };
            }
            
            return {
                success: true,
                message: 'Usuario encontrado',
                data: {
                    userId: user._id,
                    name: user.name,
                    phoneNumber: user.phoneNumber,
                    email: user.email,
                    emergencyContacts: user.emergencyContacts,
                    settings: user.settings,
                    createdAt: user.createdAt
                }
            };
        } catch (error) {
            console.error('Error in getUserByPhone:', error.message);
            throw new Error(`Error getting user profile: ${error.message}`);
        }
    },

    /**
     * Get user emergency contacts
     * @param {string} telefono - User's phone number
     * @returns {Promise<Object>} - Emergency contacts data
     */
    async getEmergencyContacts(telefono) {
        try {
            const cleanPhone = telefono.replace(/\D/g, '');
            
            const user = await UserModel.findOne({ phoneNumber: cleanPhone });
            
            if (!user) {
                return {
                    success: false,
                    message: 'Usuario no encontrado',
                    contacts: []
                };
            }
            
            return {
                success: true,
                message: 'Contactos de emergencia obtenidos',
                contacts: user.emergencyContacts || []
            };
        } catch (error) {
            console.error('Error in getEmergencyContacts:', error.message);
            throw new Error(`Error getting emergency contacts: ${error.message}`);
        }
    },

    /**
     * Add emergency contact to user
     * @param {string} telefono - User's phone number
     * @param {Object} contact - Emergency contact data
     * @returns {Promise<Object>} - Result of the operation
     */
    async addEmergencyContact(telefono, contact) {
        try {
            const cleanPhone = telefono.replace(/\D/g, '');
            
            // Validate contact data
            if (!contact.name || !contact.phone) {
                return {
                    success: false,
                    message: 'Nombre y teléfono del contacto son obligatorios'
                };
            }

            const user = await UserModel.findOne({ phoneNumber: cleanPhone });
            
            if (!user) {
                return {
                    success: false,
                    message: 'Usuario no encontrado'
                };
            }

            // Add the new contact to the array
            const newContact = {
                name: contact.name,
                phone: contact.phone,
                relationship: contact.relationship || 'Contacto de Emergencia'
            };

            user.emergencyContacts.push(newContact);
            await user.save();

            console.log(`✅ Contacto de emergencia agregado a ${user.name}: ${newContact.name}`);

            return {
                success: true,
                message: 'Contacto de emergencia agregado exitosamente',
                contact: newContact,
                totalContacts: user.emergencyContacts.length
            };
        } catch (error) {
            console.error('Error in addEmergencyContact:', error.message);
            throw new Error(`Error adding emergency contact: ${error.message}`);
        }
    },

    /**
     * Update emergency contact
     * @param {string} telefono - User's phone number
     * @param {string} contactId - Contact ID to update
     * @param {Object} updatedData - Updated contact data
     * @returns {Promise<Object>} - Result of the operation
     */
    async updateEmergencyContact(telefono, contactId, updatedData) {
        try {
            const cleanPhone = telefono.replace(/\D/g, '');
            
            const user = await UserModel.findOne({ phoneNumber: cleanPhone });
            
            if (!user) {
                return {
                    success: false,
                    message: 'Usuario no encontrado'
                };
            }

            const contact = user.emergencyContacts.id(contactId);
            
            if (!contact) {
                return {
                    success: false,
                    message: 'Contacto no encontrado'
                };
            }

            // Update contact fields
            if (updatedData.name) contact.name = updatedData.name;
            if (updatedData.phone) contact.phone = updatedData.phone;
            if (updatedData.relationship) contact.relationship = updatedData.relationship;

            await user.save();

            return {
                success: true,
                message: 'Contacto actualizado exitosamente',
                contact: contact
            };
        } catch (error) {
            console.error('Error in updateEmergencyContact:', error.message);
            throw new Error(`Error updating emergency contact: ${error.message}`);
        }
    },

    /**
     * Delete emergency contact
     * @param {string} telefono - User's phone number
     * @param {string} contactId - Contact ID to delete
     * @returns {Promise<Object>} - Result of the operation
     */
    async deleteEmergencyContact(telefono, contactId) {
        try {
            const cleanPhone = telefono.replace(/\D/g, '');
            
            const user = await UserModel.findOne({ phoneNumber: cleanPhone });
            
            if (!user) {
                return {
                    success: false,
                    message: 'Usuario no encontrado'
                };
            }

            const contact = user.emergencyContacts.id(contactId);
            
            if (!contact) {
                return {
                    success: false,
                    message: 'Contacto no encontrado'
                };
            }

            contact.deleteOne();
            await user.save();

            return {
                success: true,
                message: 'Contacto eliminado exitosamente',
                remainingContacts: user.emergencyContacts.length
            };
        } catch (error) {
            console.error('Error in deleteEmergencyContact:', error.message);
            throw new Error(`Error deleting emergency contact: ${error.message}`);
        }
    }
    
};

export default userService;