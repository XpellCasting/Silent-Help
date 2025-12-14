import express from "express";
import AlertController from "../controller/AlertController.js";

const router = express.Router();

router.post("/", AlertController.createAlert);
router.get("/:userId", AlertController.getAlertsByUser);

export default router;
