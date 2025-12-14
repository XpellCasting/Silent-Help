import express from 'express';
import UserRoutes from './UserRoutes.js';



import AlertRoutes from './AlertRoutes.js';

const router = express.Router();

router.use('/user', UserRoutes);
router.use('/alerts', AlertRoutes);


export default router;