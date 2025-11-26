import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card'
import React from 'react'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import PaymentDetailsForm from './PaymentDetailsForm'
import { useDispatch, useSelector } from 'react-redux'
import { useEffect } from 'react'
import { getPaymentDetails } from '@/State/Withdrawal/Action'

function PaymentDetails() {

    const { withdrawal } = useSelector(store => store);
    const dispatch = useDispatch();

    useEffect(() => {
        dispatch(getPaymentDetails({ jwt: localStorage.getItem("jwt") }));
    }, []);

    // Mask Account Number
    const maskAccountNumber = (num = "") => {
        if (!num) return "";
        const last4 = num.slice(-4);
        return "*".repeat(num.length - 4) + last4;
    };

    return (
        <div className='px-20'>
            <h1 className='text-3xl font-bold py-10'>Payment Details</h1>

            {withdrawal.paymentDetails ? (
                <Card>
                    <CardHeader>
                        <CardTitle>
                            {withdrawal.paymentDetails?.bankName}
                        </CardTitle>

                        <CardDescription>
                            A/C No: {maskAccountNumber(withdrawal.paymentDetails?.accountNumber)}
                        </CardDescription>
                    </CardHeader>

                    <CardContent>
                        <div className='flex items-center'>
                            <p className='w-32'>A/C Holder</p>
                            <p className='text-gray-400'>: {withdrawal.paymentDetails?.accountHolderName}</p>
                        </div>

                        <div className='flex items-center'>
                            <p className='w-32'>IFSC</p>
                            <p className='text-gray-400'>: {withdrawal.paymentDetails?.ifsc}</p>
                        </div>
                    </CardContent>
                </Card>
            ) : (
                <Dialog>
                    <DialogTrigger>
                        <Button className='py-6'>
                            Add Payment Details
                        </Button>
                    </DialogTrigger>

                    <DialogContent>
                        <DialogHeader>
                            <DialogTitle>Payment Details</DialogTitle>
                        </DialogHeader>

                        <PaymentDetailsForm />
                    </DialogContent>
                </Dialog>
            )}

        </div>
    )
}

export default PaymentDetails
