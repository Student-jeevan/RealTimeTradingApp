import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card'
import React from 'react'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import PaymentDetailsForm from './PaymentDetailsForm'
function PaymentDetails() {
    return (
        <div className='px-20'>
            <h1 className='text-3xl font-bold py-10'>Payment Details</h1>
            {true?<Card>
                <CardHeader>
                    <CardTitle>
                        Yes Bank
                    </CardTitle>
                    <CardDescription>
                        A/C:
                        *********7878

                    </CardDescription>
                </CardHeader>
                <CardContent>
                    <div className='flex items-center'>
                        <p className='w-32'>A/C Holder</p>
                        <p className='text-gray-400'>:Jeevan</p>
                    </div>
                    <div className='flex items-center'>
                        <p className='w-32'>IFSC</p>
                        <p className='text-gray-400'>:SBI0I11</p>
                        
                    </div>
                </CardContent>
            </Card>:<Dialog>
                <DialogTrigger>
                    <Button className='py-6'>
                        Add payement details
                    </Button>
                </DialogTrigger>
                <DialogContent>
                    <DialogHeader>
                    <DialogTitle>Payement Details</DialogTitle>

                    </DialogHeader>
                    <PaymentDetailsForm/>
                </DialogContent>
            </Dialog>}
           
        </div>
    )
}
export default PaymentDetails
