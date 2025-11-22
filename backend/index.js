import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import db from './config/DB.js';
import router from './routes/index.js';


dotenv.config();

const corsOptions = {
    origin: '*',
    methods: 'GET,HEAD,PUT,PATCH,POST,DELETE',
    preflightContinue: true,
    optionsSuccessStatus: 204,
};



const app = express();
const PORT = process.env.PORT || 3000;


app.use(cors(corsOptions));
app.use(express.json());



app.use('/api', router);

app.listen(PORT, () => {
    console.log(`🚀 Server is running on http://localhost:${PORT}`);
});
