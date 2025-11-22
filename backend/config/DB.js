import mongoose from 'mongoose';

class Database {
  constructor() {
    // If an instance already exists, return it (singleton guard).
    if (Database._instance) return Database._instance;

    this._connect();
    Database._instance = this;
    return Database._instance;
  }

  _connect() {
    // If we already have a live connection, skip reconnecting.
    if (this.db && mongoose.connection.readyState === 1) {
      console.log('✅ Already connected to MongoDB');
      return;
    }

    mongoose
      .connect(process.env.MONGODB_URI || 'mongodb+srv://Xpell:Ciywk1dqyRFruLo0@cluster0.en8cs8s.mongodb.net/?appName=Cluster0', {
        useNewUrlParser: true,
        useUnifiedTopology: true,
      })
      .then(() => {
        console.log('✅ MongoDB Connected Successfully');
        this.db = mongoose.connection;
      })
      .catch((error) => {
        console.error('❌ MongoDB Connection Error:', error.message);
        process.exit(1);
      });

    // Connection events
    mongoose.connection.on('connected', () => {
      console.log('📡 Mongoose connected to MongoDB');
    });

    mongoose.connection.on('error', (err) => {
      console.error('❌ Mongoose connection error:', err);
    });

    mongoose.connection.on('disconnected', () => {
      console.log('📴 Mongoose disconnected from MongoDB');
    });

    // Graceful shutdown
    process.on('SIGINT', async () => {
      await mongoose.connection.close();
      console.log('👋 MongoDB connection closed due to app termination');
      process.exit(0);
    });
  }

  static getInstance() {
    if (!Database._instance) {
      Database._instance = new Database();
    }
    return Database._instance;
  }

  getConnection() {
    return this.db;
  }

  async disconnect() {
    if (this.db) {
      await mongoose.connection.close();
      this.db = null;
      Database._instance = null;
      console.log('🔌 MongoDB disconnected');
    }
  }
}

// Export a single shared instance (ES module default export).
const instance = Database.getInstance();
export default instance;