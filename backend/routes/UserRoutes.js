import express from "express";
import UserController from "../controller/UserController.js";

const router = express.Router();

router.post("/send-code", UserController.sendCode);
router.post("/verify-code", UserController.verifyCode);
router.get("/verification-status/:telefono", UserController.getVerificationStatus);
router.post("/complete-register", UserController.completeRegistration);
router.get("/profile/:telefono", UserController.getUserProfile);
router.get("/emergency-contacts/:telefono", UserController.getEmergencyContacts);
router.post("/emergency-contacts/:telefono", UserController.addEmergencyContact);
router.put("/emergency-contacts/:telefono/:contactId", UserController.updateEmergencyContact);
router.delete("/emergency-contacts/:telefono/:contactId", UserController.deleteEmergencyContact);


export default router;