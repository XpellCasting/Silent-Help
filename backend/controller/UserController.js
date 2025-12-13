
import userService from "../services/userService.js";


const sendCode = async (req, res) => {
    try {
        const {nombre, telefono, email} = req.body;
        
        if (!nombre || !telefono ) {
            return res.status(400).json({ message: "Faltan datos obligatorios" });
        }

        console.log(`Nombre: ${nombre}, Teléfono: ${telefono}, Email: ${email}`);

        // Send verification code using Twilio
        const result = await userService.sendVerificationCode(nombre, telefono, email);

        res.status(200).json({
            success: true,
            message: result.message,
            data: result.data
        });
    } catch (error) {
        console.error("Error in sendCode:", error);
        res.status(500).json({ 
            success: false,
            message: "Error interno del servidor",
            error: error.message 
        });
    }
};

const verifyCode = async (req, res) => {
    try {
        const {telefono, codigo} = req.body;


        console.log (`Teléfono: ${telefono}, Código: ${codigo}`);
        
        if (!telefono || !codigo) {
            return res.status(400).json({ 
                success: false,
                message: "Teléfono y código son obligatorios" 
            });
        }

        // Verify the code using simple verification
        const result = await userService.verifyCode(telefono, codigo);

        res.status(200).json({
            success: result.success,
            message: result.message,
            verified: result.verified,
            userData: result.userData
        });
    } catch (error) {
        console.error("Error in verifyCode:", error);
        res.status(500).json({ 
            success: false,
            message: "Error interno del servidor",
            error: error.message 
        });
    }
};

const getVerificationStatus = async (req, res) => {
    try {
        const { telefono } = req.params;
        
        if (!telefono) {
            return res.status(400).json({ 
                success: false,
                message: "Teléfono es obligatorio" 
            });
        }

        // Get verification status
        const result = await userService.getVerificationStatus(telefono);

        res.status(200).json(result);
    } catch (error) {
        console.error("Error in getVerificationStatus:", error);
        res.status(500).json({ 
            success: false,
            message: "Error interno del servidor",
            error: error.message 
        });
    }
};

const completeRegistration = async (req, res) => {
    try {
        const { nombre, telefono, email, contactoEmergencia } = req.body;
        if (!nombre || !telefono) {
            return res.status(400).json({
                success: false,
                message: "Faltan datos obligatorios"
            });
        }
        contactoEmergencia.relationship = "Desconocido";
        const response = await userService.createUserProfile(nombre, telefono, email, contactoEmergencia);

        if (response.success) {
            return res.status(201).json(response);
        }

        return res.status(400).json({
            success: false,
            message: "Error al crear el perfil de usuario"
        }); 

    } catch (error) {
        console.error("Error in completeRegistration:", error);
        res.status(500).json({
            success: false,
            message: "Error interno del servidor",
            error: error.message
        });
    }
};

const getUserProfile = async (req, res) => {
    try {
        const { telefono } = req.params;
        
        if (!telefono) {
            return res.status(400).json({
                success: false,
                message: "El teléfono es obligatorio"
            });
        }

        const response = await userService.getUserByPhone(telefono);

        if (response.success) {
            return res.status(200).json(response);
        }

        return res.status(404).json(response);

    } catch (error) {
        console.error("Error in getUserProfile:", error);
        res.status(500).json({
            success: false,
            message: "Error interno del servidor",
            error: error.message
        });
    }
};

const getEmergencyContacts = async (req, res) => {
    try {
        const { telefono } = req.params;
        
        if (!telefono) {
            return res.status(400).json({
                success: false,
                message: "El teléfono es obligatorio"
            });
        }

        const response = await userService.getEmergencyContacts(telefono);

        return res.status(200).json(response);

    } catch (error) {
        console.error("Error in getEmergencyContacts:", error);
        res.status(500).json({
            success: false,
            message: "Error interno del servidor",
            error: error.message
        });
    }
};

const addEmergencyContact = async (req, res) => {
    try {
        const { telefono } = req.params;
        const { name, phone, relationship } = req.body;
        
        if (!telefono) {
            return res.status(400).json({
                success: false,
                message: "El teléfono del usuario es obligatorio"
            });
        }

        if (!name || !phone) {
            return res.status(400).json({
                success: false,
                message: "Nombre y teléfono del contacto son obligatorios"
            });
        }

        const contact = { name, phone, relationship };
        const response = await userService.addEmergencyContact(telefono, contact);

        if (response.success) {
            return res.status(201).json(response);
        }

        return res.status(404).json(response);

    } catch (error) {
        console.error("Error in addEmergencyContact:", error);
        res.status(500).json({
            success: false,
            message: "Error interno del servidor",
            error: error.message
        });
    }
};

const updateEmergencyContact = async (req, res) => {
    try {
        const { telefono, contactId } = req.params;
        const updatedData = req.body;
        
        if (!telefono || !contactId) {
            return res.status(400).json({
                success: false,
                message: "Teléfono y contactId son obligatorios"
            });
        }

        const response = await userService.updateEmergencyContact(telefono, contactId, updatedData);

        if (response.success) {
            return res.status(200).json(response);
        }

        return res.status(404).json(response);

    } catch (error) {
        console.error("Error in updateEmergencyContact:", error);
        res.status(500).json({
            success: false,
            message: "Error interno del servidor",
            error: error.message
        });
    }
};

const deleteEmergencyContact = async (req, res) => {
    try {
        const { telefono, contactId } = req.params;
        
        if (!telefono || !contactId) {
            return res.status(400).json({
                success: false,
                message: "Teléfono y contactId son obligatorios"
            });
        }

        const response = await userService.deleteEmergencyContact(telefono, contactId);

        if (response.success) {
            return res.status(200).json(response);
        }

        return res.status(404).json(response);

    } catch (error) {
        console.error("Error in deleteEmergencyContact:", error);
        res.status(500).json({
            success: false,
            message: "Error interno del servidor",
            error: error.message
        });
    }
};



const UserController = {
    sendCode,
    verifyCode,
    getVerificationStatus,
    completeRegistration,
    getUserProfile,
    getEmergencyContacts,
    addEmergencyContact,
    updateEmergencyContact,
    deleteEmergencyContact
};



export default UserController;