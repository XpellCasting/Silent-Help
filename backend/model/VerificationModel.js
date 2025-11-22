import mongoose from 'mongoose';

const verificationSchema = new mongoose.Schema({
    phoneNumber: {
        type: String,
        required: true,
    },
    code: {
        type: String,
        required: true,
    },
    name: {
        type: String,
        required: true,
    },
    email: {
        type: String,
    },
    createdAt: {
        type: Date,
        default: Date.now,
        expires: 300 // El documento expira automáticamente después de 5 minutos (300 segundos)
    },
    isVerified: {
        type: Boolean,
        default: false,
    }
});

// Índice para búsqueda rápida por teléfono
verificationSchema.index({ phoneNumber: 1 });

export default mongoose.model('Codigos_Verificacion', verificationSchema);