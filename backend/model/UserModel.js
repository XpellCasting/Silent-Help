import mongoose from 'mongoose';

// 🔹 Subdocumento: contacto de emergencia
const emergencyContactSchema = new mongoose.Schema({
  name: {
    type: String,
    required: [true, 'El nombre del contacto de emergencia es obligatorio'],
    trim: true,
  },
  phone: {
    type: String,
    required: [true, 'El número de teléfono es obligatorio'],
  },
  relationship: {
    type: String,
    required: [true, 'Debe indicar la relación con el contacto'],
  },
});

// 🔹 Subdocumento: configuración biométrica
const biometricSettingsSchema = new mongoose.Schema({
  fingerprintEnabled: {
    type: Boolean,
    default: false,
  },
  faceIdEnabled: {
    type: Boolean,
    default: false,
  },
  panicFingerprint: {
    enabled: {
      type: Boolean,
      default: false,
    },
    fingerIndex: {
      type: Number,
      min: 0,
      max: 10,
    },
  },
});

// 🔹 Subdocumento: configuración de sensores
const sensorSettingsSchema = new mongoose.Schema({
  gpsFrequencySeconds: {
    type: Number,
    default: 30,
    min: 5,
  },
  audioClipSeconds: {
    type: Number,
    default: 10,
    min: 5,
  },
  proximityModeEnabled: {
    type: Boolean,
    default: false,
  },
});

// 🔹 Subdocumento: gestos configurables
const gestureSettingsSchema = new mongoose.Schema({
  alertGesture: {
    type: String,
    enum: ['VOLUME_DOWN_TRIPLE_PRESS', 'SHAKE_DEVICE', 'DOUBLE_TAP_POWER'],
    default: 'VOLUME_DOWN_TRIPLE_PRESS',
  },
});

// 🔹 Subdocumento principal de settings (configuraciones del usuario)
const settingsSchema = new mongoose.Schema({
  biometrics: biometricSettingsSchema,
  sensors: sensorSettingsSchema,
  gestures: gestureSettingsSchema,
});

// 🔹 Esquema principal del usuario
const userSchema = new mongoose.Schema(
  {
    name: {
      type: String,
      required: [true, 'El nombre es obligatorio'],
      trim: true,
    },
    email: {
      type: String,
      lowercase: true,
      unique: true,
      sparse: true,
    },
    phoneNumber: {
      type: String,
      unique: true,
      required: [true, 'El número de teléfono es obligatorio'],
    },
    emergencyContacts: [emergencyContactSchema],
    settings: settingsSchema,
    createdAt: {
      type: Date,
      default: Date.now,
    },
  },
  {
    timestamps: false, // ya tienes createdAt en tu JSON
    versionKey: false, // evita "__v"
  }
);



// 🔹 Exportar modelo
export default mongoose.model('User', userSchema);
