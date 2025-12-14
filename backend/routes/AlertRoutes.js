import express from "express";
import AlertController from "../controller/AlertController.js";

const router = express.Router();

router.post('/create', AlertController.createAlert);
router.get('/:userId', AlertController.getAlertsByUser);
router.put('/:alertId/audio', AlertController.addAudioToAlert);
router.put('/:alertId/end', AlertController.endAlert);

export default router;
